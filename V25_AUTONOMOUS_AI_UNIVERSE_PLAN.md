# v25 Autonomous AI Universe

v25 promotes the existing local-first AIDI decision stack into a bounded autonomous controller. The goal is not unconstrained self-modification: the wallpaper autonomously chooses refresh cadence and adaptation mode while all rendering parameters remain inside the existing SceneDecision safety bounds.

## Runtime policy

`aidi_gateway/autonomous_policy.py` adds `autonomy-v1` metadata to every gateway decision after context and personality processing.

Modes:

- `learning` — profile is still warming up; decisions are reviewed frequently.
- `adaptive` — memory/context are useful but confidence is not yet high enough for a long autonomous interval.
- `autonomous` — mature, high-confidence state; refresh cadence may extend to the configured maximum.
- `constrained` — battery safety overrides personalization and shortens the review window.
- `recovery` — provider/router failure is present; the gateway shortens the retry window while keeping deterministic rendering alive.

The policy never enables raw camera, audio, or location data. `raw_media` remains false by contract.

## Inputs used for confidence

Only already-sanitized state is considered: provider tier, profile sample count, bounded memory recall score, semantic context confidence, battery state, and current decision source. Cloud provenance is treated as bounded and receives a shorter maximum review window than a mature local/deterministic decision.

## Release gates

1. `autonomy-v1` policy unit tests cover learning, mature autonomous mode, low-battery safety, recovery, and cloud bounding.
2. AIDI Gateway integration returns an autonomous envelope without accepting raw media.
3. Existing hybrid router, memory, context, and personality modules compile together.
4. Android APK reports versionCode 250 / versionName 25.0.
5. APK retains INTERNET only for AIDI transport and does not gain CAMERA, RECORD_AUDIO, or location permissions.
6. APK signature verification and artifact publication succeed.

## Next stage

After v25 is green, the next production-hardening release should focus on secure transport/configuration, observability, and device-side diagnostics before any broader autonomous inputs are added.
