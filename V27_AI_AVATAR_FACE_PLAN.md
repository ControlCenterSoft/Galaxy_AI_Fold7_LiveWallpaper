# v27.0 — AI Avatar Face

## Goal
Promote the avatar from a small wireframe mask to the primary living visual element of AI Living Universe while preserving offline continuity and the AIDI architecture.

## Implemented
- `AIAvatarRendererV27`: layered holographic human face with head silhouette, skin-light gradient, cheeks, eyes/iris/pupils, brows, nose, lips and AI accent.
- Smooth deterministic blink, gaze drift, breathing and micro head motion rendered locally at frame rate.
- Five bounded emotions: `calm`, `focused`, `thinking`, `happy`, `sleep`.
- Backward mapping: `aware -> thinking`, `resting -> sleep`.
- Fold-aware hero composition: larger avatar on both cover and main displays; galaxy/hologram become supporting layers.
- AIDI `personality-v2` adds `avatar.emotion` while keeping legacy `avatar.state` for old clients.
- Emotion selection remains bounded by device state and works with LocalFallbackAI when Gateway is unavailable.
- Existing AIDI Gateway, memory, context, hybrid routing, autonomy, security and privacy boundaries remain intact.

## Release gate
- Android `versionCode 270`, `versionName 27.0`.
- v27 source contract check.
- AIDI Gateway response includes one of the five supported emotions and no raw media.
- Android API 35 build succeeds.
- APK badging and INTERNET permission verified.
- CAMERA, RECORD_AUDIO and fine location stay absent.
- APK signature verifies and SHA-256 is emitted.
