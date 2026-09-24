# v62.0 — Jaw & Chin Articulation

## Goal
Make Russian TTS speech look more physically coherent by extending mouth movement into subtle jaw and chin motion while retaining the photoreal mesh-only face pipeline.

## Implementation
- Keep v61 syllabic speech articulation, v60 natural blinking, v59 saccade coupling and v58 shared gaze.
- Add a bounded lower-face mesh response driven by the existing local `mouthOpen` scalar.
- Let the chin descend slightly during open syllables and add a very small lower-cheek compensation so the mouth no longer deforms in isolation.
- Keep all deformation on original portrait pixels; no painted mouth, synthetic teeth or overlay mask is added.
- Preserve posture, gaze, Fold continuity, adaptive quality and all existing AI functions.

## Safety / privacy
- Motion is derived locally from the Russian TTS lifecycle only.
- Default AI locale remains `ru-RU` / `language=ru`.
- No microphone, camera, precise location, raw-media capture or transmission is introduced.
- AIDI Gateway, AI Memory/Qdrant contract, LocalFallbackAI and hybrid routing remain intact.
