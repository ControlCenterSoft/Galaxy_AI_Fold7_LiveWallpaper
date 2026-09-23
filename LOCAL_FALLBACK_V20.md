# v20 LocalFallbackAI / Offline Runtime

This stage makes v20 independent from AIDI Gateway availability.

## Runtime behavior

- Gateway I/O remains off the render thread.
- Any transport/protocol failure immediately produces a deterministic LocalFallbackAI decision.
- The local decision uses only the current device snapshot: battery, charging, Fold display, hour and motion.
- Offline decisions remain inside the SceneDecision safety envelope, so renderer scale, pulse, particles and avatar offsets stay bounded.
- Gateway retries use bounded exponential backoff: 60s, 120s, 240s, 480s, then at most 900s.
- Local TTL can shorten a retry interval, but retries never occur faster than the 60s minimum refresh.
- A successful Gateway request resets the failure counter and exits fallback mode automatically.

## Diagnostics

AIDIClient now exposes:

- `isInFallbackMode()`
- `getConsecutiveGatewayFailures()`
- `getFallbackSinceAtMs()`
- existing last Gateway error/latency/success timestamps

## Verification

The v20 CI runs Android local JVM tests before assembling the APK:

1. LocalFallbackAI scenario selection and deterministic repeatability.
2. Recovery/backoff bounds and reset behavior.
3. AIDIClient offline failover using an injected failing transport.
4. 25,000 controller updates across fallback modes and Fold transitions, asserting finite/bounded renderer values and 16–50 ms frame pacing.

This establishes the offline/autonomous baseline. The next independent stage can exercise live Gateway recovery and failover/recovery transition tests against the simulator.
