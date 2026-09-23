# Galaxy AI Fold7 v31 — Adaptive Portrait Presence

## Goal
Make the avatar read as a living portrait rather than a glowing mask while preserving all v20-v30 AIDI, memory, fallback, privacy, voice and Fold continuity behavior.

## Delivered
- New `AIAvatarRendererV31` with layered portrait rendering.
- Dimensional face gradient, cheek/temple/chin lighting and warmer human palette handling.
- Natural eyelids, iris rings, dual catchlights and bounded autonomous gaze.
- Upper/lower lip geometry instead of a single mouth stroke.
- Nose bridge/base, ears and neck for a more complete portrait silhouette.
- Subtle generated head yaw, breathing and vertical drift; no camera or biometric input.
- AIDI emotion states remain `calm`, `focused`, `thinking`, `happy`, `sleep`.
- Existing v28 Human/Sci-Fi/Custom style system and v30 personal warmth/eye glow/expression settings remain compatible.
- Larger, higher Fold-aware portrait composition to make the face the primary visual element on cover and main displays.

## Privacy
The renderer is fully local. It does not request camera, microphone, location or biometric permissions. Existing sanitized AIDI personalization remains unchanged.

## Release gate
- `versionCode 310`, `versionName 31.0`.
- Compile and APK build on Android SDK 35.
- Verify portrait integration tokens and inherited privacy boundary.
- Verify INTERNET is present and CAMERA/RECORD_AUDIO/ACCESS_FINE_LOCATION are absent.
- Verify APK signature and SHA-256 before artifact publication.
