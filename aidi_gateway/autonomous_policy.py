#!/usr/bin/env python3
"""Bounded autonomous scene policy for AIDI Gateway v25.

The policy does not create new sensor inputs and never accepts raw media. It only
controls confidence, refresh cadence and safety overrides for an already bounded
SceneDecision.
"""

import os

SCHEMA = "autonomy-v1"
MIN_REVIEW_SECONDS = max(30, int(os.environ.get("AIDI_AUTONOMY_MIN_REVIEW_SECONDS", "60")))
MAX_REVIEW_SECONDS = max(MIN_REVIEW_SECONDS, int(os.environ.get("AIDI_AUTONOMY_MAX_REVIEW_SECONDS", "900")))


def clamp(value, low, high):
    return max(low, min(high, value))


def _number(value, default=0.0):
    try:
        return float(value)
    except Exception:
        return float(default)


def _integer(value, default=0):
    try:
        return int(value)
    except Exception:
        return int(default)


def apply_autonomy(payload, decision):
    """Attach an explainable, safety-bounded autonomous control envelope."""
    payload = payload if isinstance(payload, dict) else {}
    result = dict(decision or {})
    state = payload.get("state") or {}
    profile = payload.get("profile") or {}
    memory = result.get("memory") or {}
    context = result.get("context") or {}
    provenance = result.get("provenance") or {}

    battery = clamp(_integer(state.get("battery", 50), 50), 0, 100)
    charging = bool(state.get("charging", False))
    low_battery = battery <= 15 and not charging
    samples = max(0, _integer(profile.get("samples", 0), 0))
    recall_score = clamp(_number(memory.get("recall_score", 0.0), 0.0), 0.0, 1.0)
    context_confidence = clamp(_number(context.get("confidence", 0.0), 0.0), 0.0, 1.0)
    provider_tier = str(provenance.get("tier", "deterministic"))[:24]
    source = str(result.get("source", "unknown"))[:64]

    sample_confidence = clamp(samples / 12.0, 0.0, 1.0)
    provider_confidence = {
        "deterministic": 0.82,
        "local": 0.90,
        "cloud": 0.86,
    }.get(provider_tier, 0.65)
    confidence = clamp(
        0.50 * provider_confidence
        + 0.22 * sample_confidence
        + 0.16 * recall_score
        + 0.12 * context_confidence,
        0.0,
        1.0,
    )

    safety_override = False
    reason = "bounded_adaptation"
    if low_battery:
        mode = "constrained"
        review_seconds = 180
        confidence = max(confidence, 0.80)
        safety_override = True
        reason = "low_battery_safety"
    elif "fallback" in source or "error" in source:
        mode = "recovery"
        review_seconds = MIN_REVIEW_SECONDS
        reason = "provider_recovery"
    elif samples < 5:
        mode = "learning"
        review_seconds = 120
        reason = "profile_warmup"
    elif confidence >= 0.78:
        mode = "autonomous"
        review_seconds = MAX_REVIEW_SECONDS
        reason = "high_confidence"
    else:
        mode = "adaptive"
        review_seconds = 300

    if provider_tier == "cloud":
        review_seconds = min(review_seconds, 600)
        reason = reason + ":cloud_bounded"

    review_seconds = int(clamp(review_seconds, MIN_REVIEW_SECONDS, MAX_REVIEW_SECONDS))
    current_ttl = max(1, _integer(result.get("ttl", review_seconds), review_seconds))
    if mode in ("recovery", "learning", "constrained"):
        effective_ttl = min(current_ttl, review_seconds)
    else:
        effective_ttl = review_seconds
    result["ttl"] = int(clamp(effective_ttl, MIN_REVIEW_SECONDS, MAX_REVIEW_SECONDS))
    result["autonomy"] = {
        "schema": SCHEMA,
        "mode": mode,
        "confidence": round(confidence, 4),
        "reason": reason,
        "review_after_seconds": review_seconds,
        "safety_override": safety_override,
        "raw_media": False,
    }
    return result


def health():
    return {
        "schema": SCHEMA,
        "enabled": True,
        "min_review_seconds": MIN_REVIEW_SECONDS,
        "max_review_seconds": MAX_REVIEW_SECONDS,
        "raw_media_accepted": False,
        "modes": ["learning", "adaptive", "autonomous", "constrained", "recovery"],
    }
