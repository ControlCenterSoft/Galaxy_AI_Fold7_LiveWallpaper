#!/usr/bin/env python3
"""Bounded avatar personality policy for AIDI Gateway v23."""


def clamp(value, low=0.0, high=1.0):
    return max(low, min(high, value))


def _number(value, default):
    try:
        return float(value)
    except Exception:
        return float(default)


def defaults_for_state(state):
    state = str(state or "calm")[:32]
    if state == "focused":
        return {"presence": 0.82, "serenity": 0.42, "curiosity": 0.62, "focus": 0.92}
    if state == "aware":
        return {"presence": 0.72, "serenity": 0.58, "curiosity": 0.74, "focus": 0.66}
    if state == "resting":
        return {"presence": 0.38, "serenity": 0.92, "curiosity": 0.18, "focus": 0.20}
    return {"presence": 0.56, "serenity": 0.80, "curiosity": 0.38, "focus": 0.44}


def apply_personality(payload, decision):
    result = dict(decision)
    avatar = dict(result.get("avatar") or {})
    state = str(avatar.get("state", "calm"))[:32]
    traits = defaults_for_state(state)

    profile = payload.get("profile") or {}
    profile_traits = profile.get("personality") or {}
    samples = max(0, int(profile.get("samples", 0) or 0))
    if samples >= 3 and isinstance(profile_traits, dict):
        weight = min(0.24, 0.08 + min(samples, 20) * 0.008)
        for key, default in list(traits.items()):
            remembered = clamp(_number(profile_traits.get(key, default), default))
            traits[key] = round(default * (1.0 - weight) + remembered * weight, 5)

    avatar["state"] = state
    avatar["traits"] = {key: round(clamp(value), 5) for key, value in traits.items()}
    avatar["schema"] = "personality-v1"
    result["avatar"] = avatar
    return result
