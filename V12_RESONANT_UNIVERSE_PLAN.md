# Galaxy AI Fold7 v12.0 — Resonant Universe Engine

Release goal: extend v11 Adaptive Universe with a Fold-transition resonance layer that briefly increases visual energy during main/cover display changes and then decays back to the adaptive long-session profile.

## Implemented scope

- preserves v9/v10/v11 personal universe identity and evolution continuity
- new `ResonantUniverseControllerV12` wraps the v11 adaptive engine
- detects main/cover display changes without additional permissions
- temporary scene-scale, avatar-drift, pulse and particle resonance after Fold transitions
- transition-time render cadence boost followed by v11 long-session pacing
- no cloud/API dependency for runtime behavior
- Android 35 / Java 17 / Gradle 8.10.2 build path

## Release gates

1. Android package metadata reports versionName 12.0 / versionCode 120.
2. Debug APK builds on Java 17 + Android SDK 35 + Gradle 8.10.2.
3. APK signature verification passes.
4. Workflow uploads `Galaxy_AI_Fold7_LiveWallpaper_v12.0.apk`.
5. SHA-256 is printed in CI output.
6. v12 branch is based on the successful v11 Adaptive Universe lineage.

## Branch

`release/v12-resonant-universe`
