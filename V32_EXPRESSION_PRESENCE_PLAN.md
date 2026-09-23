# Galaxy AI Fold7 v32 — Expression Presence Engine

## Goal
Make the v31 portrait feel continuously attentive and context-aware without adding invasive sensors or breaking the existing AIDI/Memory/Fallback/Voice/Personalization stack.

## Delivered
- New `PortraitPresenceControllerV32`, implemented as a platform-free bounded state machine.
- Smoothly blends AIDI `presence`, `focus`, `curiosity`, `energy` and emotion into portrait scale, vertical placement, micro horizontal drift, glow and particle density.
- Emotion-specific presence behavior for `calm`, `focused`, `thinking`, `happy` and `sleep`.
- Fold-aware composition: cover display receives slightly stronger portrait prominence while main display remains less crowded.
- Sleep mode reduces visual activity and raises the frame delay to the safe 50 ms ceiling; focused/happy modes can request a 32 ms frame cadence when the universe budget permits it.
- Existing v31 local blink/gaze/head motion remains intact and is now orchestrated by a higher-level presence envelope.
- No camera, microphone, biometric, location or raw media input is introduced.

## Compatibility
- Preserves AIDI Gateway integration, AI Memory/Qdrant protocol, LocalFallbackAI, hybrid routing, privacy/security, ambient voice, v28 style themes and v30 personalization.
- `SceneDecision` protocol remains backward compatible; v32 consumes existing bounded fields only.

## Release gate
- `versionCode 320`, `versionName 32.0`.
- JVM bounds/transition test for the new presence controller.
- Android SDK 35 compile and APK build.
- Verify INTERNET is present while CAMERA/RECORD_AUDIO/ACCESS_FINE_LOCATION remain absent.
- Verify APK signature and SHA-256 before artifact publication.
