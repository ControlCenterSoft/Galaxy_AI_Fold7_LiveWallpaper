# v20 AIDI Gateway Simulator — implementation status

Status: **implemented and CI-gated**

This stage turns the repository simulator into a deterministic compatibility target for the Android `AIDIClient` and the production AIDI Gateway.

## Implemented

- dependency-free HTTP simulator for `scene-v1`;
- configurable `--host` and `--port`;
- health endpoint with protocol/service identity;
- deterministic `decision_id` derived from the canonical request;
- request ID correlation through JSON and `X-AIDI-Request-Id`;
- bounded Scene Decision output compatible with Android `SceneDecision`;
- explicit handling for unsupported protocol, malformed JSON, unsupported media type, oversized payload and unknown endpoints;
- state sequence echo for integration diagnostics.

## Automated protocol tests

`gateway_simulator/test_scene_protocol.py` starts the simulator on an ephemeral local port and verifies:

1. `/health` contract;
2. low-battery decision -> `deep_black/resting`;
3. night-idle decision -> `deep_nebula/calm`;
4. charging-active decision -> `energy_core/focused`;
5. main-display decision -> `living_continuum/aware`;
6. cover-display decision -> `compact_continuum/calm`;
7. all Android Scene Decision bounds;
8. deterministic response identity;
9. request/header correlation;
10. protocol mismatch rejection;
11. request ID mismatch rejection;
12. malformed JSON rejection;
13. non-JSON media type rejection;
14. 404 behavior.

The v20 GitHub Actions workflow runs this suite before Android/Gradle compilation, then also starts the standalone simulator and probes its health endpoint. APK build and artifact publication therefore occur only after the Scene Decision contract is green.

## Next independent stage

Exercise runtime failover: `Gateway -> LocalFallbackAI -> Gateway recovery`, including stale decision TTL, retry/backoff and renderer continuity.
