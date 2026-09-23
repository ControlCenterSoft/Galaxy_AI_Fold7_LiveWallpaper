# Galaxy AI Fold7 v23.0 — AI Avatar Personality

v23 makes the avatar a persistent behavioral layer rather than a fixed visual element.

## Implemented architecture

- `SceneDecision` now carries bounded personality traits: `presence`, `serenity`, `curiosity`, and `focus`.
- Older v20-v22 decisions remain binary compatible through deterministic defaults derived from `avatar.state`.
- `AIProfileMemory` upgrades its transmitted summary to `profile-v2` and keeps EWMA personality traits alongside scene preferences.
- `AIPersonalityControllerV23` interpolates traits over time and translates them into avatar scale, glow, hologram intensity, aura particles and subtle drift.
- AIDI Gateway fast-path responses are enriched with `personality-v1`; prior `profile-v2` personality acts only as a bounded soft prior.
- The existing local fallback remains functional because personality defaults are derived from fallback avatar state when the Gateway is offline.
- v22 semantic context and v21 local/Qdrant scene memory remain active.

## Release gates

1. `versionCode=230`, `versionName=23.0`.
2. Legacy SceneDecision constructor remains available.
3. Gateway personality policy emits all four traits in range 0..1.
4. `profile-v2` contains bounded personality averages.
5. Wallpaper renderer actually consumes personality values.
6. Android APK builds, signature verification passes, and SHA-256 is emitted.
7. APK artifact is uploaded.

## Next release

v24 will harden AIDI hybrid routing: fast deterministic path, local LLM, optional cloud escalation, provider health/circuit breakers, and explicit provenance in every scene decision.
