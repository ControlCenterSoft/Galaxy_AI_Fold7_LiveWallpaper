# Galaxy AI Fold7 v21.0 — AI Memory Universe

v21 turns the v20 AI control loop into a persistent, privacy-bounded personal universe.

## Implemented architecture

- On-device `AIProfileMemory` stores only bounded visual preference aggregates and counters.
- No raw motion history, prompts, account data or precise location are persisted by the app memory layer.
- Each AIDI request can include a `profile-v1` summary.
- AIDI Gateway v0.21 combines three layers in priority order:
  1. battery/night safety baseline;
  2. bounded on-device profile prior;
  3. similar historical scene decisions from AIDI memory.
- `MemoryStore` provides an in-process rolling memory and optional Qdrant persistence/search.
- Qdrant is configured through `/etc/aidi-gateway.env`; secrets are not stored in the repository.
- Local LLM responses are written to memory after successful normalization.
- v21 renderer interpolates scene parameters to prevent visible jumps between remembered and newly generated decisions.

## Qdrant configuration

Example `/etc/aidi-gateway.env` on the Gateway host:

```text
AIDI_QDRANT_URL=http://qdrant.service:6333
AIDI_QDRANT_COLLECTION=galaxy_ai_scene_memory_v21
# AIDI_QDRANT_API_KEY=...   # only when Qdrant authentication is enabled
```

With `AIDI_QDRANT_URL` empty, the Gateway remains fully functional with bounded in-process memory.

## Release gates

1. `versionCode=210`, `versionName=21.0`.
2. Python memory smoke test passes without external Qdrant.
3. Scene protocol integration test includes `profile-v1` and memory metadata.
4. Android APK builds on SDK 35 / Java 17.
5. APK signature verification passes.
6. APK artifact is uploaded and SHA-256 is emitted.

## Build status

The v21 workflow is registered. This commit intentionally triggers the first full v21 memory + gateway + APK validation run.

## Next release

v22 will add a privacy-gated Context/Vision layer. It must remain opt-in and must not require camera access for normal wallpaper operation.
