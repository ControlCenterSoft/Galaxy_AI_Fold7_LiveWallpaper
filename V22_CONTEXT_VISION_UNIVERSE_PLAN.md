# Galaxy AI Fold7 v22.0 — Context Vision Universe

v22 extends the living AI loop with privacy-bounded environmental awareness while keeping normal live-wallpaper operation camera-free.

## Implemented architecture

- `AmbientContextCollector` observes the Android ambient light sensor only while the wallpaper is visible.
- Exact lux samples are never stored or transmitted; they are immediately reduced to `dark`, `dim`, `normal`, or `bright`.
- `AIContextSignal` sends only `context-v1` semantic/coarse fields and explicitly marks `raw_media=false`.
- No `CAMERA`, microphone, location, media-library or contacts permission is added.
- AIDI Gateway v0.22 applies context as a bounded soft prior. Low-battery safety remains authoritative.
- Gateway policy rejects any `raw_media=true` context from the scene endpoint and substitutes a non-media policy signal.
- Local LLM receives only sanitized `context-v1`, memory and device state.
- Existing v21 local/Qdrant memory and v20 offline fallback remain active.

## Why this is the v22 Vision foundation

The wallpaper engine itself should not continuously capture camera frames. v22 defines the semantic context boundary that a separately consented vision component can later feed after local analysis. The scene engine therefore gains environmental awareness now without introducing a dangerous permission or raw-media upload path.

## Release gates

1. `versionCode=220`, `versionName=22.0`.
2. Context policy smoke tests pass for dark/bright and raw-media rejection.
3. Gateway integration returns `context-v1` metadata.
4. Android source contains ambient sensor lifecycle handling.
5. APK declares INTERNET but does not declare CAMERA, RECORD_AUDIO or location permissions.
6. APK signature verification passes and SHA-256 is emitted.
7. APK artifact is uploaded.

## Next release

v23 will turn the avatar state into a persistent AI personality layer driven by bounded memory, context and AIDI decisions.
