# AIDI Gateway Integration Layer — v20

This stage establishes the production boundary between the wallpaper runtime and AIDI Gateway.

## Runtime path

`GalaxyAIWallpaperService -> AIDIClient -> AIDIGatewayTransport -> HttpAIDIGatewayTransport -> AIDI Gateway`

If the gateway request fails, `AIDIClient` switches to `LocalFallbackAI` without blocking rendering.

## Protocol

Endpoint: `POST /api/v1/scene/analyze`

Client headers:

- `Content-Type: application/json; charset=utf-8`
- `Accept: application/json`
- `X-AIDI-Protocol: scene-v1`
- `X-AIDI-Client: galaxy-fold7-live-wallpaper`

Payload remains the v20 `AIState` envelope with device, app version and current device state.

## Reliability rules

- gateway I/O runs off the render thread;
- only one request may be in flight;
- remote TTL is clamped to 60 seconds..60 minutes;
- failed remote calls retry on a bounded backoff rather than waiting for a long local TTL;
- request/response bodies have size limits;
- local fallback remains active when the gateway is unavailable;
- the HTTP transport is injectable through `AIDIGatewayTransport`, enabling a deterministic simulator in the next integration stage.

## Security boundary

No API key or reusable secret is embedded in the APK. Device authentication/pairing is intentionally left to the real AIDI Gateway deployment stage. The production endpoint is HTTPS.

## Completion criteria for this stage

- transport abstraction present;
- production HTTP transport present;
- renderer remains non-blocking;
- fallback path preserved;
- bounded payloads and TTLs enforced;
- CI build required after this change.
