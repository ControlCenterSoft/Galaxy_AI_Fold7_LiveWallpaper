#!/usr/bin/env python3
"""Bounded avatar personality and emotion policy for AIDI Gateway v23+."""

from autonomous_policy import apply_autonomy


def clamp(value, low=0.0, high=1.0):
    return max(low, min(high, value))


def _number(value, default):
    try:
        return float(value)
    except Exception:
        return float(default)


def normalize_emotion(state):
    value = str(state or "calm")[:32].strip().lower()
    if value == "aware":
        return "thinking"
    if value == "resting":
        return "sleep"
    if value in {"calm", "focused", "thinking", "happy", "sleep"}:
        return value
    return "calm"


def defaults_for_state(state):
    state = normalize_emotion(state)
    if state == "focused":
        return {"presence": 0.82, "serenity": 0.42, "curiosity": 0.62, "focus": 0.92}
    if state == "thinking":
        return {"presence": 0.72, "serenity": 0.58, "curiosity": 0.78, "focus": 0.66}
    if state == "happy":
        return {"presence": 0.86, "serenity": 0.72, "curiosity": 0.58, "focus": 0.46}
    if state == "sleep":
        return {"presence": 0.38, "serenity": 0.92, "curiosity": 0.18, "focus": 0.20}
    return {"presence": 0.56, "serenity": 0.80, "curiosity": 0.38, "focus": 0.44}


def derive_emotion(payload, state):
    state = normalize_emotion(state)
    device_state = payload.get("state") or {}
    battery = _number(device_state.get("battery", 100), 100)
    charging = bool(device_state.get("charging", False))
    hour = int(_number(device_state.get("hour", 12), 12)) % 24
    motion = _number(device_state.get("motion_level", 0.0), 0.0)

    if not charging and battery <= 15:
        return "sleep"
    if state in {"focused", "thinking", "happy", "sleep"}:
        return state
    if 0 <= hour < 6 and motion < 0.2:
        return "sleep"
    if charging and battery >= 80:
        return "happy"
    if motion > 0.45:
        return "focused"
    if motion > 0.12:
        return "thinking"
    return "calm"


def apply_personality(payload, decision):
    result = dict(decision)
    avatar = dict(result.get("avatar") or {})
    raw_state = str(avatar.get("state", "calm"))[:32]
    emotion = derive_emotion(payload, raw_state)
    traits = defaults_for_state(emotion)

    profile = payload.get("profile") or {}
    profile_traits = profile.get("personality") or {}
    samples = max(0, int(profile.get("samples", 0) or 0))
    if samples >= 3 and isinstance(profile_traits, dict):
        weight = min(0.24, 0.08 + min(samples, 20) * 0.008)
        for key, default in list(traits.items()):
            remembered = clamp(_number(profile_traits.get(key, default), default))
            traits[key] = round(default * (1.0 - weight) + remembered * weight, 5)

    # Keep legacy state for older Android clients and add a v27 emotion field for the living face.
    avatar["state"] = raw_state
    avatar["emotion"] = emotion
    avatar["traits"] = {key: round(clamp(value), 5) for key, value in traits.items()}
    avatar["schema"] = "personality-v2"
    result["avatar"] = avatar
    return apply_autonomy(payload, result)
