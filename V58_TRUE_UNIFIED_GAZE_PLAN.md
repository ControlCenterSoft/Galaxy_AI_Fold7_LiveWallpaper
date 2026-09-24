# v58.0 — True Unified Gaze

## Goal
Remove the remaining split-brain between the portrait eye mesh and head-follow controller. The same gaze vector must drive both systems so the assistant looks and turns as one living subject rather than two independent animations.

## Implementation
- Keep `AttentionGazeControllerV46` as the single service-level gaze source.
- After each gaze update, feed its normalized X/Y into `DeformableLivePortraitV45.setGaze(...)`.
- Feed the exact same X/Y into `HeadEyeCoordinationV49.update(...)`.
- Preserve touch attention, natural fixation/saccades, posture inertia, Fold motion continuity, Russian TTS/lip motion and all inherited AI layers.

## Safety / privacy
- Default AI locale remains `ru-RU` / `language=ru`.
- No microphone, camera, precise location or raw-media capture/transmission is introduced.
- AIDI Gateway, AI Memory/Qdrant contract, LocalFallbackAI and hybrid routing stay intact.
