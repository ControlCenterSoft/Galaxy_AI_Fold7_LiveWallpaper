# Galaxy AI Fold7 v9.0 — AI Personal Universe Engine

Release goal: evolve the Neural Reality track into a personalized, device-adaptive live universe while keeping the wallpaper engine offline-first and battery-aware.

## Scope

- Personal Universe scene profiles and deterministic seeds
- Avatar/world state persistence
- Fold cover/main-display scene continuity
- Adaptive quality profiles for thermal, battery and refresh-rate conditions
- Scene preset import/export groundwork
- Renderer telemetry hooks for frame pacing and fallback quality
- Backward-compatible upgrade path from v8.0

## Release gates

1. Android package metadata reports versionName 9.0 / versionCode 90.
2. Debug APK builds on Java 17 + Android SDK 35 + Gradle 8.10.2.
3. APK signature verification passes.
4. Workflow uploads `Galaxy_AI_Fold7_LiveWallpaper_v9.0.apk`.
5. SHA-256 is printed in CI output.

## Branch

`release/v9-ai-personal-universe`
