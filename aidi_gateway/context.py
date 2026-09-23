#!/usr/bin/env python3
"""Privacy-bounded environmental context policy for AIDI Gateway v22+v23."""

from personality import apply_personality


def clamp(value, low, high):
    return max(low, min(high, value))


def _number(value, default=0.0):
    try:
        return float(value)
    except Exception:
        return float(default)


def sanitize_context(payload):
    raw = payload.get("context") or {}
    if not isinstance(raw, dict) or raw.get("schema") != "context-v1":
        return {
            "schema": "context-v1",
            "light": "unknown",
            "semantic_label": "unknown",
            "confidence": 0.0,
            "source": "none",
            "raw_media": False,
        }

    if bool(raw.get("raw_media", False)):
        return {
            "schema": "context-v1",
            "light": "unknown",
            "semantic_label": "rejected_raw_media",
            "confidence": 0.0,
            "source": "policy",
            "raw_media": False,
        }

    light = str(raw.get("light", "unknown"))[:16]
    if light not in ("dark", "dim", "normal", "bright", "unknown"):
        light = "unknown"
    return {
        "schema": "context-v1",
        "light": light,
        "semantic_label": str(raw.get("semantic_label", "unknown"))[:48],
        "confidence": clamp(_number(raw.get("confidence", 0.0)), 0.0, 1.0),
        "source": str(raw.get("source", "none"))[:32],
        "raw_media": False,
    }


def apply_context(payload, decision, low_battery=False):
    context = sanitize_context(payload)
    result = dict(decision)
    scene = dict(result.get("scene") or {})
    confidence = context["confidence"]
    light = context["light"]

    if not low_battery and confidence >= 0.50:
        energy = clamp(_number(scene.get("energy", 0.45)), 0.0, 1.0)
        particles = clamp(_number(scene.get("particle_multiplier", 1.0)), 0.45, 1.8)
        pulse = clamp(_number(scene.get("pulse_multiplier", 1.0)), 0.6, 1.5)
        if light == "dark":
            energy *= 0.88
            particles *= 0.90
            pulse *= 0.94
        elif light == "dim":
            energy *= 0.94
            particles *= 0.96
        elif light == "bright":
            energy = min(1.0, energy * 1.05)
            particles = min(1.8, particles * 1.04)
        scene["energy"] = round(clamp(energy, 0.0, 1.0), 5)
        scene["particle_multiplier"] = round(clamp(particles, 0.45, 1.8), 5)
        scene["pulse_multiplier"] = round(clamp(pulse, 0.6, 1.5), 5)

    result["scene"] = scene
    result["context"] = {
        "schema": "context-v1",
        "light": context["light"],
        "semantic_label": context["semantic_label"],
        "confidence": context["confidence"],
        "source": context["source"],
        "raw_media": False,
        "applied": bool(not low_battery and confidence >= 0.50),
    }
    return apply_personality(payload, result)
