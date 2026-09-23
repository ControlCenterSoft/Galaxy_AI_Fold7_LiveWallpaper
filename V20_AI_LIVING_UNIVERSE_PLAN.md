# Galaxy AI Fold7 v20.0 — AI Living Universe

v20 is the first release where the deterministic universe renderer is controlled by a live AI decision channel rather than only mathematical evolution.

## Implemented architecture

- `AIDIClient` — asynchronous production gateway client.
- `AIStateCollector` — live battery, charging, Fold display, time and motion context.
- `SceneDecision` — bounded gateway decision contract.
- `LocalFallbackAI` — autonomous operation when AIDI is offline.
- `LivingUniverseControllerV20` — safe adaptation layer over v19 Event Horizon Universe.
- `AIDI Scene Protocol v1` — stable request/response API.
- A stdlib Python AIDI Gateway simulator for CI integration tests.

## Production behavior

1. Wallpaper starts immediately from the local cached/bootstrap state.
2. State is collected locally.
3. `AIDIClient` requests a scene decision asynchronously from `aidi-gateway.hm.dm`.
4. A valid response is clamped and cached for the returned TTL.
5. Rendering remains local and battery-aware.
6. Gateway failure automatically switches to `LocalFallbackAI` without interrupting rendering.

## Release gates

1. Gateway simulator `/health` succeeds.
2. Scene Protocol v1 smoke request returns a bounded decision.
3. Android build succeeds on Java 17 / Android SDK 35 / Gradle 8.10.2.
4. APK reports `versionCode=200`, `versionName=20.0`.
5. APK contains Internet permission for AIDI connectivity.
6. APK signature verification succeeds.
7. SHA-256 is recorded and artifact is uploaded.

## Next track

v21 adds durable AI memory/profile state and prepares the production AIDI Gateway memory API while retaining all v20 offline guarantees.
