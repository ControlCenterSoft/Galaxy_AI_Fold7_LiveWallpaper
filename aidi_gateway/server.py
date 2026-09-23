#!/usr/bin/env python3
import json
import os
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from memory import MemoryStore

HOST = os.environ.get("AIDI_GATEWAY_HOST", "0.0.0.0")
PORT = int(os.environ.get("AIDI_GATEWAY_PORT", "8088"))
OLLAMA_URL = os.environ.get("AIDI_OLLAMA_URL", "http://192.168.10.218:11434/api/chat")
MODEL = os.environ.get("AIDI_OLLAMA_MODEL", "qwen3-coder:30b")
LLM_CACHE_SECONDS = int(os.environ.get("AIDI_LLM_CACHE_SECONDS", "900"))

_executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="aidi-llm")
_lock = threading.Lock()
_cache = {}
_pending = set()
_memory = MemoryStore()
_stats = {
    "requests": 0,
    "llm_started": 0,
    "llm_success": 0,
    "llm_error": 0,
    "started_at": int(time.time()),
}


def clamp(value, low, high):
    return max(low, min(high, value))


def _number(value, default):
    try:
        return float(value)
    except Exception:
        return float(default)


def _memory_metadata(payload, memory_hint):
    profile = payload.get("profile") or {}
    return {
        "profile_samples": max(0, int(profile.get("samples", 0) or 0)),
        "recall_backend": str((memory_hint or {}).get("_memory_backend", "none"))[:24],
        "recall_score": clamp(_number((memory_hint or {}).get("_memory_score", 0.0), 0.0), 0.0, 1.0),
    }


def baseline(payload, memory_hint=None):
    state = payload.get("state") or {}
    profile = payload.get("profile") or {}
    battery = int(state.get("battery", 50))
    charging = bool(state.get("charging", False))
    hour = int(state.get("hour", 12))
    motion = float(state.get("motion_level", 0.0))
    fold_state = state.get("fold_state", "cover")
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

    # Local device memory is only a gentle preference prior. Safety/battery rules always win.
    profile_samples = max(0, int(profile.get("samples", 0) or 0))
    if not low_battery and profile_samples >= 3:
        preferred_energy = clamp(_number(profile.get("average_energy", energy), energy), 0.0, 1.0)
        preferred_particles = clamp(
            _number(profile.get("average_particles", particles), particles), 0.45, 1.8
        )
        energy = energy * 0.90 + preferred_energy * 0.10
        particles = particles * 0.90 + preferred_particles * 0.10

    # Similar historical decisions from AIDI/Qdrant provide a second bounded prior.
    remembered_scene = (memory_hint or {}).get("scene") if isinstance(memory_hint, dict) else None
    remembered_scene = remembered_scene if isinstance(remembered_scene, dict) else {}
    memory_score = clamp(_number((memory_hint or {}).get("_memory_score", 0.0), 0.0), 0.0, 1.0)
    if not low_battery and memory_score >= 0.86 and remembered_scene:
        weight = min(0.22, max(0.08, (memory_score - 0.82) * 0.55))
        energy = energy * (1.0 - weight) + clamp(
            _number(remembered_scene.get("energy", energy), energy), 0.0, 1.0
        ) * weight
        particles = particles * (1.0 - weight) + clamp(
            _number(remembered_scene.get("particle_multiplier", particles), particles), 0.45, 1.8
        ) * weight
        pulse = pulse * (1.0 - weight) + clamp(
            _number(remembered_scene.get("pulse_multiplier", pulse), pulse), 0.6, 1.5
        ) * weight
        scale = scale * (1.0 - weight) + clamp(
            _number(remembered_scene.get("scene_scale", scale), scale), 0.97, 1.03
        ) * weight

    return {
        "decision_id": "fast-%d" % int(time.time()),
        "request_id": str(payload.get("request_id", ""))[:160],
        "scene": {
            "name": name,
            "energy": round(clamp(energy, 0.0, 1.0), 5),
            "particle_multiplier": round(clamp(particles, 0.45, 1.8), 5),
            "pulse_multiplier": round(clamp(pulse, 0.6, 1.5), 5),
            "scene_scale": round(clamp(scale, 0.97, 1.03), 6),
            "avatar_x_bias": 0.001 if fold_state == "main" else 0.0,
            "avatar_y_bias": -0.001 if night else 0.0,
        },
        "avatar": {"state": avatar},
        "ttl": 60,
        "source": "aidi-fast-path-v21",
        "memory": _memory_metadata(payload, memory_hint),
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

    name = str(scene_in.get("name", base_scene["name"]))[:64]
    avatar_state = str(avatar_in.get("state", fallback["avatar"]["state"]))[:32]

    return {
        "decision_id": "llm-%d" % int(time.time()),
        "request_id": fallback.get("request_id", ""),
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
        "memory": fallback.get("memory", {}),
    }


def ollama_decision(payload, fallback):
    system = """You are the AIDI scene decision engine for Galaxy AI Fold7 live wallpaper.
Return JSON only. Choose a calm, battery-aware visual state from device context.
The request can contain profile and memory_context. Treat them as soft preferences only;
battery safety and low-motion night behavior have priority.
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
    return str(payload.get("device", "GalaxyFold7"))


def _model_payload(payload, memory_hint):
    result = dict(payload)
    if memory_hint:
        context = dict(memory_hint)
        context.pop("request_id", None)
        context.pop("decision_id", None)
        result["memory_context"] = context
    return result


def refresh_llm(key, payload, fallback, memory_hint):
    with _lock:
        _stats["llm_started"] += 1
    try:
        decision = ollama_decision(_model_payload(payload, memory_hint), fallback)
        _memory.remember(payload, decision, "local-llm")
        with _lock:
            _cache[key] = (time.time(), decision)
            _stats["llm_success"] += 1
    except Exception as exc:
        with _lock:
            _stats["llm_error"] += 1
            failed = dict(fallback)
            failed["source"] = "aidi-llm-fallback"
            failed["llm_error"] = str(exc)[:160]
            _cache[key] = (time.time(), failed)
    finally:
        with _lock:
            _pending.discard(key)


def decide(payload):
    memory_hint = _memory.recall(payload)
    fallback = baseline(payload, memory_hint)
    key = cache_key(payload)
    now = time.time()
    with _lock:
        _stats["requests"] += 1
        cached = _cache.get(key)
        fresh = cached and now - cached[0] < LLM_CACHE_SECONDS
        if fresh:
            decision = dict(cached[1])
            decision["request_id"] = fallback.get("request_id", "")
            decision["memory"] = fallback.get("memory", {})
            return decision
        if key not in _pending:
            _pending.add(key)
            _executor.submit(refresh_llm, key, payload, fallback, memory_hint)
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
        "ollama_url": OLLAMA_URL,
        "pending": pending,
        "cached_devices": cache_devices,
        "memory": _memory.stats(),
        "stats": stats,
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "AIDI-Gateway/0.21"

    def send_json(self, code, body, request_id=""):
        data = json.dumps(body, separators=(",", ":")).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-AIDI-Protocol", "scene-v1")
        if request_id:
            self.send_header("X-AIDI-Request-Id", request_id[:160])
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
        request_id = self.headers.get("X-AIDI-Request-Id", "")
        if self.headers.get("X-AIDI-Protocol", "scene-v1") != "scene-v1":
            self.send_json(400, {"error": "unsupported_protocol"}, request_id)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 65536:
                raise ValueError("invalid content length")
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            body_request_id = str(payload.get("request_id", ""))
            if request_id and body_request_id and request_id != body_request_id:
                raise ValueError("request id mismatch")
            request_id = request_id or body_request_id
            self.send_json(200, decide(payload), request_id)
        except Exception as exc:
            self.send_json(400, {"error": "bad_request", "detail": str(exc)[:200]}, request_id)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print("AIDI Gateway v21 listening on %s:%d, model=%s" % (HOST, PORT, MODEL), flush=True)
    server.serve_forever()
