#!/usr/bin/env python3
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def clamp(value, low, high):
    return max(low, min(high, value))


def decide(payload):
    state = payload.get("state") or {}
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

    return {
        "decision_id": "sim-v20",
        "scene": {
            "name": name,
            "energy": clamp(energy, 0.0, 1.0),
            "particle_multiplier": particles,
            "pulse_multiplier": pulse,
            "scene_scale": scale,
            "avatar_x_bias": 0.001 if fold_state == "main" else 0.0,
            "avatar_y_bias": -0.001 if night else 0.0,
        },
        "avatar": {"state": avatar},
        "ttl": 300,
        "source": "aidi-gateway-simulator",
    }


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, code, body):
        data = json.dumps(body, separators=(",", ":")).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {"status": "ok", "protocol": "scene-v1"})
        else:
            self._send_json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/api/v1/scene/analyze":
            self._send_json(404, {"error": "not_found"})
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(size) or b"{}")
            self._send_json(200, decide(payload))
        except Exception as exc:
            self._send_json(400, {"error": "bad_request", "detail": str(exc)})

    def log_message(self, fmt, *args):
        return


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8765), Handler)
    print("AIDI Gateway simulator listening on http://127.0.0.1:8765", flush=True)
    server.serve_forever()
