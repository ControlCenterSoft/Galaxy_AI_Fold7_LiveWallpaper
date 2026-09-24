# Galaxy AI Fold7 v63.0 — Russian Coarticulation

## Goal
Make Russian TTS-driven facial articulation flow more naturally between syllables without microphone/audio capture and without drawing a synthetic mouth over the photoreal portrait.

## Changes
- Preserve v61 pseudo-syllabic timing and v62 jaw/chin mesh articulation.
- Add interpolation between previous and current articulation targets (coarticulation).
- Add short deterministic word-boundary micro-pauses so speech is not a continuous jaw flap.
- Keep asymmetric opening/closing inertia and emotion-aware cadence.
- Continue to drive only the local portrait mesh through `getMouthOpen()`.

## Permanent contracts
- Default AI locale remains `ru-RU`; language remains `ru` for Gateway and local fallback.
- Preserve AIDI Gateway, AI Memory/Qdrant, LocalFallbackAI and hybrid routing.
- Preserve Fold continuity and all previous live portrait behavior.
- No camera, microphone, precise location or raw-media transfer.
- Backward compatible with v62 behavior when TTS is idle.

## Release gate
CI must verify versionCode 630/versionName 63.0, Russian locale contract, privacy permissions, successful APK signature verification, SHA-256 output and GitHub artifact upload.
