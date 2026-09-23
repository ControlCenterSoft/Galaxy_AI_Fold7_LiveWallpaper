#!/usr/bin/env python3
import json
import os
import sqlite3
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = os.environ.get("AIDI_GATEWAY_HOST", "0.0.0.0")
PORT = int(os.environ.get("AIDI_GATEWAY_PORT", "8088"))
OLLAMA_URL = os.environ.get("AIDI_OLLAMA_URL", "http://192.168.10.218:11434/api/chat")
MODEL = os.environ.get("AIDI_OLLAMA_MODEL", "qwen3-coder:30b")
LLM_CACHE_SECONDS = int(os.environ.get("AIDI_LLM_CACHE_SECONDS", "900"))
LLM_ENABLED = os.environ.get("AIDI_LLM_ENABLED", "1").strip().lower() not in ("0", "false", "no")
MEMORY_DB = os.environ.get("AIDI_MEMORY_DB", "/opt/aidi-gateway/aidi_memory.db")

_executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="aidi-llm")
_lock = threading.Lock()
_cache = {}
_pending = set()
_stats = {
    "requests": 0,
    "llm_started": 0,
    "llm_success": 0,
    "llm_error": 0,
    "memory_reads": 0,
    "memory_writes": 0,
    "started_at": int(time.time()),
}


def clamp(value, low, high):
    return max(low, min(high, value))


def safe_token(value, fallback, limit=64):
    text = str(value or "").strip()
    if not text:
        text = fallback
    text = "".join(ch if ch.isalnum() or ch in "_.-" else "_" for ch in text)
    return text[:limit] or fallback


def db_connect():
    parent = os.path.dirname(MEMORY_DB)
    if parent:
        os.makedirs(parent, exist_ok=True)
    connection = sqlite3.connect(MEMORY_DB, timeout=5.0)
    connection.execute("PRAGMA journal_mode=WAL")
    connection.execute("PRAGMA synchronous=NORMAL")
    return connection


def init_memory_db():
    with db_connect() as db:
        db.execute(
            """
            CREATE TABLE IF NOT EXISTS device_memory (
                device TEXT PRIMARY KEY,
                decision_count INTEGER NOT NULL DEFAULT 0,
                last_scene TEXT NOT NULL DEFAULT 'continuum',
                last_avatar TEXT NOT NULL DEFAULT 'calm',
                last_source TEXT NOT NULL DEFAULT 'bootstrap',
                favorite_scene TEXT NOT NULL DEFAULT 'continuum',
                favorite_scene_count INTEGER NOT NULL DEFAULT 0,
                scene_counts TEXT NOT NULL DEFAULT '{}',
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """
        )


def load_memory(device):
    try:
        with db_connect() as db:
            row = db.execute(
                "SELECT decision_count,last_scene,last_avatar,last_source,"
                "favorite_scene,favorite_scene_count,updated_at "
                "FROM device_memory WHERE device=?",
                (device,),
            ).fetchone()
        with _lock:
            _stats["memory_reads"] += 1
        if not row:
            return {
                "decision_count": 0,
                "last_scene": "continuum",
                "last_avatar": "calm",
                "favorite_scene": "continuum",
                "favorite_scene_count": 0,
                "age_seconds": -1,
            }
        age = max(0, int(time.time()) - int(row[6])) if int(row[6]) > 0 else -1
        return {
            "decision_count": int(row[0]),
            "last_scene": row[1],
            "last_avatar": row[2],
            "last_source": row[3],
            "favorite_scene": row[4],
            "favorite_scene_count": int(row[5]),
            "age_seconds": age,
        }
    except Exception:
        return {
            "decision_count": 0,
            "last_scene": "continuum",
            "last_avatar": "calm",
            "favorite_scene": "continuum",
            "favorite_scene_count": 0,
            "age_seconds": -1,
        }


def store_memory(device, decision):
    scene = safe_token((decision.get("scene") or {}).get("name"), "continuum")
    avatar = safe_token((decision.get("avatar") or {}).get("state"), "calm", 32)
    source = safe_token(decision.get("source"), "unknown", 96)
    now = int(time.time())
    try:
        with db_connect() as db:
            row = db.execute(
                "SELECT decision_count,scene_counts FROM device_memory WHERE device=?",
                (device,),
            ).fetchone()
            total = int(row[0]) if row else 0
            try:
                counts = json.loads(row[1]) if row and row[1] else {}
            except Exception:
                counts = {}
            counts[scene] = min(1000000, int(counts.get(scene, 0)) + 1)
            favorite_scene, favorite_count = max(
                counts.items(), key=lambda item: int(item[1])
            )
            total = min(1000000, total + 1)
            db.execute(
                """
                INSERT INTO device_memory(
                    device,decision_count,last_scene,last_avatar,last_source,
                    favorite_scene,favorite_scene_count,scene_counts,updated_at
                ) VALUES(?,?,?,?,?,?,?,?,?)
                ON CONFLICT(device) DO UPDATE SET
                    decision_count=excluded.decision_count,
                    last_scene=excluded.last_scene,
                    last_avatar=excluded.last_avatar,
                    last_source=excluded.last_source,
                    favorite_scene=excluded.favorite_scene,
                    favorite_scene_count=excluded.favorite_scene_count,
                    scene_counts=excluded.scene_counts,
                    updated_at=excluded.updated_at
                """,
                (
                    device,
                    total,
                    scene,
                    avatar,
                    source,
                    favorite_scene,
                    int(favorite_count),
                    json.dumps(counts, separators=(",", ":"), sort_keys=True),
                    now,
                ),
            )
        with _lock:
            _stats["memory_writes"] += 1
    except Exception:
        # Memory is advisory. A storage failure must never interrupt wallpaper decisions.
        return


def memory_device_count():
    try:
        with db_connect() as db:
            return int(db.execute("SELECT COUNT(*) FROM device_memory").fetchone()[0])
    except Exception:
        return 0


def baseline(payload):
    state = payload.get("state") or {}
    battery = int(state.get("battery", 50))
    charging = bool(state.get("charging", False))
    hour = int(state.get("hour", 12))
    motion = float(state.get("motion_level", 0.0))
    fold_state = state.get("fold_state", "cover")
    memory = payload.get("memory") or {}
    night = hour >= 22 or hour < 7
    low_battery = battery <= 15 and not charging

    if low_battery:
        name, avatar, energy = "deep_black", "resting", 0.12
        particles, pulse, scale = 0.55, 0.72, 0.995
    elif night and motion < 0.45:
        name, avatar, energy = "deep_nebula", "calm", 0.24
        particles, pulse, scale = 0.72, 0.82, 0.998
    elif charging and motion > 0.55:
        name, avatar, energy = "energy_core", "focused", 0.82
        particles, pulse, scale = 1.34, 1.22, 1.006
    elif fold_state == "main":
        name, avatar, energy = "living_continuum", "aware", 0.52
        particles, pulse, scale = 1.08, 1.04, 1.002
    else:
        name, avatar, energy = "compact_continuum", "calm", 0.38
        particles, pulse, scale = 0.92, 0.96, 0.999

    # Memory only nudges neutral daytime states; safety/battery states always win.
    if not low_battery and not night and motion < 0.35:
        favorite = safe_token(memory.get("favorite_scene"), "continuum")
        favorite_count = int(memory.get("favorite_scene_count", 0) or 0)
        if favorite_count >= 3 and favorite not in ("continuum", "deep_black"):
            name = favorite
            energy = clamp(energy * 0.95, 0.15, 0.75)

    return {
        "decision_id": "fast-%d" % int(time.time()),
        "scene": {
            "name": name,
            "energy": energy,
            "particle_multiplier": particles,
            "pulse_multiplier": pulse,
            "scene_scale": scale,
            "avatar_x_bias": 0.001 if fold_state == "main" else 0.0,
            "avatar_y_bias": -0.001 if night else 0.0,
        },
        "avatar": {"state": avatar},
        "ttl": 60,
        "source": "aidi-memory-fast-path",
    }


def normalize(candidate, fallback):
    scene_in = candidate.get("scene") if isinstance(candidate, dict) else None
    avatar_in = candidate.get("avatar") if isinstance(candidate, dict) else None
    scene_in = scene_in if isinstance(scene_in, dict) else {}
    avatar_in = avatar_in if isinstance(avatar_in, dict) else {}
    base_scene = fallback["scene"]

    def num(name, default):
        try:
            return float(scene_in.get(name, default))
        except Exception:
            return float(default)

    name = safe_token(scene_in.get("name"), base_scene["name"])
    avatar_state = safe_token(avatar_in.get("state"), fallback["avatar"]["state"], 32)

    return {
        "decision_id": "llm-%d" % int(time.time()),
        "scene": {
            "name": name,
            "energy": clamp(num("energy", base_scene["energy"]), 0.0, 1.0),
            "particle_multiplier": clamp(num("particle_multiplier", base_scene["particle_multiplier"]), 0.45, 1.8),
            "pulse_multiplier": clamp(num("pulse_multiplier", base_scene["pulse_multiplier"]), 0.6, 1.5),
            "scene_scale": clamp(num("scene_scale", base_scene["scene_scale"]), 0.97, 1.03),
            "avatar_x_bias": clamp(num("avatar_x_bias", base_scene["avatar_x_bias"]), -0.02, 0.02),
            "avatar_y_bias": clamp(num("avatar_y_bias", base_scene["avatar_y_bias"]), -0.02, 0.02),
        },
        "avatar": {"state": avatar_state},
        "ttl": 900,
        "source": "aidi-local-llm:%s" % MODEL,
    }


def ollama_decision(payload, fallback):
    system = """You are the AIDI scene decision engine for Galaxy AI Fold7 live wallpaper.
Return JSON only. Choose a calm, battery-aware visual state from current device context and the supplied aggregate memory.
Memory is advisory: never let a preference override low-battery or night safety behavior.
Use this exact shape:
{"scene":{"name":"...","energy":0.0,"particle_multiplier":1.0,"pulse_multiplier":1.0,"scene_scale":1.0,"avatar_x_bias":0.0,"avatar_y_bias":0.0},"avatar":{"state":"calm"}}
Constraints: energy 0..1, particle_multiplier 0.45..1.8, pulse_multiplier 0.6..1.5,
scene_scale 0.97..1.03, avatar biases -0.02..0.02.
Do not add prose, markdown or extra keys."""
    request_body = {
        "model": MODEL,
        "stream": False,
        "format": "json",
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps(payload, separators=(",", ":"))},
        ],
        "options": {"temperature": 0.15, "num_predict": 220},
    }
    raw = json.dumps(request_body).encode("utf-8")
    req = urllib.request.Request(
        OLLAMA_URL,
        data=raw,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=90) as response:
        body = json.loads(response.read().decode("utf-8"))
    content = body.get("message", {}).get("content", "{}")
    candidate = json.loads(content)
    return normalize(candidate, fallback)


def cache_key(payload):
    return safe_token(payload.get("device"), "GalaxyFold7", 96)


def enrich_payload(payload, memory):
    enriched = dict(payload)
    # Client profile is already aggregate-only; server memory is likewise a summary.
    enriched["memory"] = memory
    return enriched


def refresh_llm(key, payload, fallback):
    with _lock:
        _stats["llm_started"] += 1
    try:
        decision = ollama_decision(payload, fallback)
        store_memory(key, decision)
        with _lock:
            _cache[key] = (time.time(), decision)
            _stats["llm_success"] += 1
    except Exception as exc:
        failed = dict(fallback)
        failed["source"] = "aidi-llm-fallback"
        failed["llm_error"] = str(exc)[:160]
        store_memory(key, failed)
        with _lock:
            _stats["llm_error"] += 1
            _cache[key] = (time.time(), failed)
    finally:
        with _lock:
            _pending.discard(key)


def decide(payload):
    key = cache_key(payload)
    memory = load_memory(key)
    enriched = enrich_payload(payload, memory)
    fallback = baseline(enriched)
    now = time.time()

    with _lock:
        _stats["requests"] += 1
        cached = _cache.get(key)
        fresh = cached and now - cached[0] < LLM_CACHE_SECONDS
        if fresh:
            return cached[1]

    if not LLM_ENABLED:
        fallback["ttl"] = 300
        fallback["source"] = "aidi-memory-fast-path"
        store_memory(key, fallback)
        return fallback

    with _lock:
        if key not in _pending:
            _pending.add(key)
            _executor.submit(refresh_llm, key, enriched, fallback)
    return fallback


def health():
    with _lock:
        stats = dict(_stats)
        pending = sorted(_pending)
        cache_devices = sorted(_cache)
    return {
        "status": "ok",
        "service": "aidi-gateway",
        "version": "0.21",
        "protocol": "scene-v1",
        "model": MODEL,
        "llm_enabled": LLM_ENABLED,
        "ollama_url": OLLAMA_URL,
        "memory_backend": "sqlite",
        "memory_devices": memory_device_count(),
        "pending": pending,
        "cached_devices": cache_devices,
        "stats": stats,
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "AIDI-Gateway/0.21"

    def send_json(self, code, body):
        data = json.dumps(body, separators=(",", ":")).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, health())
        else:
            self.send_json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/api/v1/scene/analyze":
            self.send_json(404, {"error": "not_found"})
            return
        if self.headers.get("X-AIDI-Protocol", "scene-v1") != "scene-v1":
            self.send_json(400, {"error": "unsupported_protocol"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 65536:
                raise ValueError("invalid content length")
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            self.send_json(200, decide(payload))
        except Exception as exc:
            self.send_json(400, {"error": "bad_request", "detail": str(exc)[:200]})

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


if __name__ == "__main__":
    init_memory_db()
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(
        "AIDI Gateway v21 listening on %s:%d, model=%s, memory=%s"
        % (HOST, PORT, MODEL, MEMORY_DB),
        flush=True,
    )
    server.serve_forever()
