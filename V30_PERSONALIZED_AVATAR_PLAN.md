# v30.0 — Personalized Avatar

## Goal
Make the living avatar personally configurable while keeping identity and sensitive information on-device.

## Implemented
- `AIPersonalizationProfileV30` stores a local profile id plus bounded appearance/expression/voice parameters.
- Three presets: Soft, Balanced and Vivid; users can also keep v28 visual styles independently.
- Personal profile reset regenerates the local id and restores balanced values without deleting learned AIDI Memory/Qdrant history.
- Runtime uses expression intensity for avatar scale/particles, eye glow for aura strength, warmth for hologram intensity, and personalized pitch/rate for local TextToSpeech.
- `AIState` now reports the actual `BuildConfig.VERSION_NAME` instead of a stale hard-coded app version.
- AIDI request state includes `avatar-personalization-v1` with only coarse bounded preference values. The local profile id, names, accounts, raw media and sensor history are explicitly excluded.
- `AIDIClient` merges personalization with existing `profile-v2` learned memory, context signals, local fallback and secure gateway transport.
- v27 emotions, v28 styles and v29 opt-in voice remain backward compatible.

## Release gate
- Android `versionCode 300`, `versionName 30.0`.
- Source contract verifies local profile reset, presets, sanitized Gateway schema and dynamic app version.
- Gateway integration accepts the personalization extension and preserves privacy/autonomy invariants.
- APK metadata, forbidden permissions, signature and SHA-256 are verified.
