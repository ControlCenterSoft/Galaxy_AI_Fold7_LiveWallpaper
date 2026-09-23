# Galaxy AI Fold7 v34 — Russian Context Presence

## Goal
Turn the Russian-first AI personality into a visible, non-intrusive on-wallpaper presence without adding sensitive sensors or new permissions.

## Delivered
- Russian-only contextual status cards beneath the living portrait.
- Context derives only from existing safe scene traits: emotion, focus, curiosity and energy.
- Three rotating local phrase variants per core state to reduce visual repetition.
- Fold-aware overlay width and typography for cover and main displays.
- Local switch to show or hide contextual AI statuses.
- Russian ru-RU remains the default phone AI language and AIDI locale contract.
- Voice remains opt-in Android TTS; LocalFallbackAI and AIDI Gateway stay intact.

## Privacy contract
No microphone permission, camera permission, precise location permission, account identity or raw media collection is introduced. The context-presence layer runs from scene values already present in the application.

## Release gates
- JVM test verifies Russian text for calm/focused/thinking/happy/sleep.
- Android SDK 35 build.
- versionCode 340 / versionName 34.0.
- APK signature verification.
- Permission regression gate.
- SHA-256 generation and GitHub artifact upload.
