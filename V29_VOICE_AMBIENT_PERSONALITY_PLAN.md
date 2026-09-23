# v29.0 — Voice & Ambient Personality

## Goal
Give the living avatar an optional ambient voice and bounded contextual reactions without introducing microphone capture or cloud-only dependencies.

## Implemented
- `AmbientPersonalityControllerV29` uses Android TextToSpeech locally.
- Voice is opt-in and disabled by default.
- No microphone, speech recognition or audio recording path is used.
- Three reaction levels: Quiet, Normal, Expressive with minimum reaction intervals.
- Reactions map to the five v27 emotions and continue to work with LocalFallbackAI decisions.
- v29 settings UI controls voice enable/disable and reaction level while preserving v28 style controls.
- Android 11+ TTS service discovery is declared through `<queries>` only.
- Existing AIDI Gateway, AI Memory/Qdrant, hybrid routing, autonomy, privacy/security and Fold continuity are preserved.

## Release gate
- Android `versionCode 290`, `versionName 29.0`.
- Voice source contract requires TextToSpeech and opt-in storage.
- APK must not request `RECORD_AUDIO`, CAMERA or fine location.
- Android API 35 build succeeds.
- APK metadata, signature and SHA-256 are verified.
