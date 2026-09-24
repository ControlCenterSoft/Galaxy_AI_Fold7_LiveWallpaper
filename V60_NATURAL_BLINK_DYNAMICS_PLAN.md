# v60.0 — Natural Blink Dynamics

## Goal
Make the photoreal assistant's blinking feel less mechanical while preserving the v59 gaze/blink coupling and all existing live portrait behavior.

## Implementation
- Keep v58 single-source gaze and v59 saccade-triggered blink coupling.
- Add state-aware blink duration: faster in focused/thinking states, softer in calm/happy states.
- Add subtle left/right eyelid timing asymmetry using only the existing bitmap mesh.
- Add occasional deterministic double-blink follow-up after a natural blink, with long cooldown to prevent repetitive behavior.
- Preserve the periodic blink cycle, mesh-only facial deformation, TTS mouth articulation, posture inertia, Fold continuity and adaptive rendering.

## Safety / privacy
- Blink timing is generated locally from animation state only.
- Default AI locale remains `ru-RU` / `language=ru`.
- No microphone, camera, precise location, raw-media capture or transmission is introduced.
- AIDI Gateway, AI Memory/Qdrant contract, LocalFallbackAI and hybrid routing remain intact.
