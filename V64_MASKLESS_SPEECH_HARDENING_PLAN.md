# Galaxy AI Fold7 v64.0 — Maskless Speech Pipeline Hardening

## Goal
Eliminate the dormant legacy synthetic mouth/foreground speech renderer so future changes cannot accidentally reintroduce a painted mouth or facial overlay over the photoreal AI portrait.

## Changes
- `SpeechFaceSyncV41` is now articulation-state only: no Canvas/Paint/Path/gradient rendering code.
- Russian v63 coarticulation, pseudo-syllabic timing and word-boundary micro-pauses are preserved.
- v62 jaw/chin and lip deformation continue to use only the original photoreal portrait bitmap mesh.
- Add CI assertions preventing a `speechFace.draw(...)` call and legacy graphics imports from returning.

## Permanent contracts
- AI locale `ru-RU`, language `ru`, Russian statuses/TTS/local fallback.
- Preserve AIDI Gateway, AI Memory/Qdrant, LocalFallbackAI and hybrid routing.
- Preserve Fold continuity, gaze, blink, posture, breathing and all previous live portrait behavior.
- No camera, microphone, precise location or raw-media transfer.

## Release gate
CI verifies versionCode 640/versionName 64.0, maskless speech architecture, inherited photoreal portrait checksum, Russian locale contract, privacy permissions, APK signature, SHA-256 and GitHub artifact upload.
