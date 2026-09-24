# v59.0 — Saccade Blink Coupling

## Goal
Make the assistant's gaze feel less synthetic by coupling occasional brief blinks to sufficiently fast gaze transitions, while retaining the existing natural periodic blink cycle.

## Implementation
- Keep v58 single-source gaze for both portrait eye mesh and head-follow.
- Derive a bounded local gaze-motion signal from consecutive shared gaze vectors.
- When a fast saccade crosses the threshold, advance one natural texture blink.
- Enforce a cooldown so one long attention shift cannot cause repeated blinking.
- Preserve periodic blinks, mesh-only facial deformation, TTS lip motion, posture inertia and Fold continuity.

## Safety / privacy
- The coupling uses only locally generated gaze state; no camera or sensor-based eye tracking.
- Default AI locale remains `ru-RU` / `language=ru`.
- No microphone, camera, precise location or raw-media capture/transmission is introduced.
- AIDI Gateway, AI Memory/Qdrant contract, LocalFallbackAI and hybrid routing remain intact.
