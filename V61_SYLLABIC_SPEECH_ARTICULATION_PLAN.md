# v61.0 — Syllabic Speech Articulation

## Goal
Make Russian TTS mouth motion read as natural syllabic articulation instead of a fixed-frequency flap, while preserving the photoreal mesh-only face pipeline.

## Implementation
- Keep v60 natural blink dynamics, v59 saccade coupling and v58 shared gaze.
- Replace the fixed speech harmonic with a deterministic local syllable envelope whose hold time varies between short phoneme-like intervals.
- Drive only the existing portrait mouth mesh through `setMouthOpen`; no microphone or audio-buffer analysis is used.
- Smooth opening and closing with asymmetric inertia so consonant-like closures and vowel-like openings remain subtle.
- Preserve Russian TTS lifecycle, posture, gaze, Fold continuity, adaptive quality and all existing AI functions.

## Safety / privacy
- Articulation is synthesized locally from TTS lifecycle/activity only.
- Default AI locale remains `ru-RU` / `language=ru`.
- No microphone, camera, precise location, raw-media capture or transmission is introduced.
- AIDI Gateway, AI Memory/Qdrant contract, LocalFallbackAI and hybrid routing remain intact.
