#!/usr/bin/env python3
import argparse
import hashlib
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PROTOCOL = "scene-v1"
SOURCE = "aidi-gateway-simulator"
MAX_BODY_BYTES = 65536
MAX_REQUEST_ID = 160


def clamp(value, low, high):
    return max(low, min(high, value))


def _number(value, default, low, high):
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        parsed = float(default)
    return clamp(parsed, low, high)


def _integer(value, default, low, high):
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = int(default)
    return int(clamp(parsed, low, high))


def _canonical(payload):
    return json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def _decision_id(payload):
    digest = hashlib.sha256(_canonical(payload).encode("utf-8")).hexdigest()[:16]
    return "sim-v20-" + digest


def validate_payload(payload):
    if not isinstance(payload, dict):
        raise ValueError("payload must be a JSON object")
    state = payload.get("state")
    if state is not None and not isinstance(state, dict):
        raise ValueError("state must be a JSON object")
    request_id = str(payload.get("request_id", ""))
    if len(request_id) > MAX_REQUEST_ID:
        raise ValueError("request_id too long")


def decide(payload):
    validate_payload(payload)
    state = payload.get("state") or {}
    battery = _integer(state.get("battery", 50), 50, 0, 100)
    charging = bool(state.get("charging", False))
    hour = _integer(state.get("hour", 12), 12, 0, 23)
    motion = _number(state.get("motion_level", 0.0), 0.0, 0.0, 1.0)
    fold_state = str(state.get("fold_state", "cover"))[:32]

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
        "decision_id": _decision_id(payload),
        "request_id": str(payload.get("request_id", ""))[:MAX_REQUEST_ID],
        "scene": {
            "name": name,
            "energy": clamp(energy, 0.0, 1.0),
            "particle_multiplier": clamp(particles, 0.45, 1.8),
            "pulse_multiplier": clamp(pulse, 0.6, 1.5),
            "scene_scale": clamp(scale, 0.97, 1.03),
            "avatar_x_bias": 0.001 if fold_state == "main" else 0.0,
            "avatar_y_bias": -0.001 if night else 0.0,
        },
        "avatar": {"state": avatar},
        "ttl": 300,
        "source": SOURCE,
        "state_sequence": int(payload.get("state_sequence", 0) or 0),
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "AIDI-Gateway-Simulator/0.20"

    def _send_json(self, code, body, request_id=""):
        data = json.dumps(body, separators=(",", ":")).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-AIDI-Protocol", PROTOCOL)
        if request_id:
            self.send_header("X-AIDI-Request-Id", request_id[:MAX_REQUEST_ID])
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/health":
            self._send_json(
                200,
                {"status": "ok", "service": SOURCE, "protocol": PROTOCOL},
            )
        else:
            self._send_json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/api/v1/scene/analyze":
            self._send_json(404, {"error": "not_found"})
            return

        request_id = self.headers.get("X-AIDI-Request-Id", "")
        protocol = self.headers.get("X-AIDI-Protocol", PROTOCOL)
        if protocol != PROTOCOL:
            self._send_json(400, {"error": "unsupported_protocol"}, request_id)
            return

        try:
            size = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self._send_json(400, {"error": "bad_request", "detail": "invalid content length"}, request_id)
            return

        if size <= 0:
            self._send_json(400, {"error": "bad_request", "detail": "empty body"}, request_id)
            return
        if size > MAX_BODY_BYTES:
            self._send_json(413, {"error": "payload_too_large"}, request_id)
            return

        content_type = self.headers.get("Content-Type", "")
        if "application/json" not in content_type.lower():
            self._send_json(415, {"error": "unsupported_media_type"}, request_id)
            return

        try:
            payload = json.loads(self.rfile.read(size).decode("utf-8"))
            validate_payload(payload)
            body_request_id = str(payload.get("request_id", ""))
            if request_id and body_request_id and request_id != body_request_id:
                raise ValueError("request id mismatch")
            request_id = request_id or body_request_id
            self._send_json(200, decide(payload), request_id)
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as exc:
            self._send_json(
                400,
                {"error": "bad_request", "detail": str(exc)[:200]},
                request_id,
            )

    def log_message(self, fmt, *args):
        return


def create_server(host="127.0.0.1", port=8765):
    return ThreadingHTTPServer((host, port), Handler)


def main():
    parser = argparse.ArgumentParser(description="AIDI Scene Protocol v1 gateway simulator")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    server = create_server(args.host, args.port)
    host, port = server.server_address[:2]
    print("AIDI Gateway simulator listening on http://%s:%d" % (host, port), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
