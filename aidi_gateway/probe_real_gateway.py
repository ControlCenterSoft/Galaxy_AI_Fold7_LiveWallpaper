#!/usr/bin/env python3
"""Probe a real or locally staged AIDI Gateway using the scene-v1 contract."""

import argparse
import json
import sys
import urllib.error
import urllib.request

PROTOCOL = "scene-v1"


def fetch_json(req, timeout):
    with urllib.request.urlopen(req, timeout=timeout) as response:
        body = json.loads(response.read().decode("utf-8"))
        return response.status, dict(response.headers.items()), body


def validate_decision(body):
    if not isinstance(body, dict):
        raise AssertionError("decision is not an object")
    scene = body.get("scene")
    avatar = body.get("avatar")
    if not isinstance(scene, dict) or not isinstance(avatar, dict):
        raise AssertionError("scene/avatar missing")
    required = ["name", "energy", "particle_multiplier", "pulse_multiplier", "scene_scale"]
    for key in required:
        if key not in scene:
            raise AssertionError("scene.%s missing" % key)
    energy = float(scene["energy"])
    particles = float(scene["particle_multiplier"])
    pulse = float(scene["pulse_multiplier"])
    scale = float(scene["scene_scale"])
    if not 0.0 <= energy <= 1.0:
        raise AssertionError("energy out of range")
    if not 0.45 <= particles <= 1.8:
        raise AssertionError("particle_multiplier out of range")
    if not 0.6 <= pulse <= 1.5:
        raise AssertionError("pulse_multiplier out of range")
    if not 0.97 <= scale <= 1.03:
        raise AssertionError("scene_scale out of range")
    ttl = int(body.get("ttl", 0))
    if ttl <= 0:
        raise AssertionError("ttl must be positive")


def post_scene(base_url, request_id, state, timeout, require_correlation):
    payload = {
        "device": "GalaxyFold7",
        "request_id": request_id,
        "state": state,
    }
    req = urllib.request.Request(
        base_url.rstrip("/") + "/api/v1/scene/analyze",
        data=json.dumps(payload, separators=(",", ":")).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-AIDI-Protocol": PROTOCOL,
            "X-AIDI-Client": "galaxy-fold7-live-wallpaper-probe",
            "X-AIDI-Request-Id": request_id,
        },
        method="POST",
    )
    status, headers, body = fetch_json(req, timeout)
    if status != 200:
        raise AssertionError("scene endpoint returned HTTP %s" % status)
    validate_decision(body)

    response_protocol = headers.get("X-AIDI-Protocol")
    if response_protocol and response_protocol != PROTOCOL:
        raise AssertionError("protocol response header mismatch")

    header_request_id = headers.get("X-AIDI-Request-Id")
    body_request_id = body.get("request_id")
    correlated = header_request_id == request_id or body_request_id == request_id
    if require_correlation and not correlated:
        raise AssertionError("gateway did not correlate request_id")
    return body, correlated


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://192.168.10.214:8088")
    parser.add_argument("--timeout", type=float, default=4.0)
    parser.add_argument("--require-correlation", action="store_true")
    args = parser.parse_args()

    base = args.base_url.rstrip("/")
    health_req = urllib.request.Request(base + "/health", headers={"Accept": "application/json"})
    try:
        status, _, health = fetch_json(health_req, args.timeout)
        if status != 200 or health.get("status") != "ok":
            raise AssertionError("health endpoint is not ok")
        if health.get("protocol") != PROTOCOL:
            raise AssertionError("health protocol mismatch")

        scenarios = [
            (
                "probe-main",
                {
                    "battery": 72,
                    "charging": False,
                    "hour": 15,
                    "motion_level": 0.2,
                    "fold_state": "main",
                    "state_sequence": 1001,
                    "captured_at_ms": 1790166000000,
                    "display": {"width_px": 2176, "height_px": 1812},
                },
            ),
            (
                "probe-low-battery",
                {
                    "battery": 9,
                    "charging": False,
                    "hour": 1,
                    "motion_level": 0.0,
                    "fold_state": "cover",
                    "state_sequence": 1002,
                    "captured_at_ms": 1790166060000,
                    "display": {"width_px": 968, "height_px": 2376},
                },
            ),
        ]

        results = []
        for request_id, state in scenarios:
            decision, correlated = post_scene(
                base, request_id, state, args.timeout, args.require_correlation
            )
            results.append(
                {
                    "request_id": request_id,
                    "scene": decision["scene"]["name"],
                    "source": decision.get("source", "unknown"),
                    "correlated": correlated,
                }
            )

        print(json.dumps({"health": health, "scenarios": results}, indent=2, sort_keys=True))
        return 0
    except (AssertionError, urllib.error.URLError, TimeoutError, ValueError) as exc:
        print("AIDI gateway probe failed: %s" % exc, file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
