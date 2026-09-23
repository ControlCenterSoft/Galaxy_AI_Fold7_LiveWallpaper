# Galaxy AI Fold7 v21.0 — Durable AI Memory

v21 turns the live v20 AIDI decision loop into a persistent personal universe that can remember aggregate preferences across app and gateway restarts without retaining raw sensor history.

## Implemented

- `AIProfile` and `AIProfileStore` persist a privacy-minimal on-device summary in `SharedPreferences`.
- Android sends aggregate memory (`last_scene`, favorite scene, decision counters and coarse age) with each AIDI request.
- `AIDIClient` records one profile update per completed gateway/fallback decision, never per rendered frame.
- AIDI Gateway persists per-device aggregate scene memory in SQLite with WAL enabled.
- Gateway memory is injected into the local Qwen decision context and can gently bias neutral scenes.
- Battery/night safety rules continue to override learned preferences.
- Memory failures are advisory and cannot stop the wallpaper renderer or gateway decisions.

## Privacy boundary

No raw motion/battery history, exact event timeline, contacts, location, media, account data or personal text is stored by the v21 memory layer. The memory model is intentionally limited to aggregate wallpaper behavior.

## Release gates

1. `aidi_gateway/server.py` compiles with Python stdlib only.
2. SQLite memory survives multiple scene requests and reports at least one remembered device.
3. Android build succeeds on Java 17 / Android SDK 35 / Gradle 8.10.2.
4. APK reports `versionCode=210`, `versionName=21.0`.
5. INTERNET permission remains present and APK v2 signature verifies.
6. APK SHA-256 and artifact digest are recorded.

## Next track

v22 introduces semantic/vector memory using the already available local embedding model, while SQLite remains the authoritative durable fallback store.
