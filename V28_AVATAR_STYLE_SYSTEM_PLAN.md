# v28.0 — Avatar Style System

## Goal
Let the same living AI personality render in distinct visual identities without resetting emotion, memory or Fold state.

## Implemented
- Persistent `AvatarStyleV28` palette model.
- Three selectable styles: Human, Sci-Fi and Custom Aurora.
- `AvatarStyleControllerV28` stores the selected theme independently from AI state.
- `AIAvatarRendererV28` keeps the v27 emotion/blink/gaze model but changes face palette, hologram intensity and accents by style.
- Main app now contains a lightweight style picker and live-wallpaper launcher.
- Fold cover/main composition remains consistent and style changes are picked up live.
- v27 emotion protocol, AIDI Gateway, Memory/Qdrant, LocalFallbackAI, hybrid routing, autonomy and secure endpoint boundary stay backward compatible.

## Release gate
- Android `versionCode 280`, `versionName 28.0`.
- Source contract verifies HUMAN, SCI_FI and CUSTOM styles and persistent preference key.
- Android API 35 build succeeds.
- APK metadata, permissions, signature and SHA-256 verified.
