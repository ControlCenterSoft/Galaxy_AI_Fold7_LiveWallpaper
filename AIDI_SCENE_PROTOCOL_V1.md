# AIDI Scene Protocol v1

Galaxy AI Fold7 LiveWallpaper v20 communicates with AIDI Gateway through a small, cache-friendly decision protocol. Rendering always stays local; the gateway only changes bounded scene parameters.

## Endpoint

`POST /api/v1/scene/analyze`

Android sends header `X-AIDI-Protocol: scene-v1`.

Every state exchange also carries `request_id` in the JSON body and `X-AIDI-Request-Id` in the HTTP headers. The gateway must return the same correlation ID in both places.

## Request

```json
{
  "device": "GalaxyFold7",
  "app_version": "20.0",
  "request_id": "fold7-42",
  "captured_at_ms": 1770000000000,
  "state_sequence": 42,
  "state": {
    "battery": 82,
    "charging": false,
    "fold_state": "main",
    "hour": 22,
    "motion_level": 0.18,
    "display": {
      "width_px": 2176,
      "height_px": 1812,
      "orientation": "landscape"
    }
  }
}
```

## Response

```json
{
  "decision_id": "aidi-001245",
  "request_id": "fold7-42",
  "scene": {
    "name": "deep_nebula",
    "energy": 0.35,
    "particle_multiplier": 0.8,
    "pulse_multiplier": 0.9,
    "scene_scale": 0.998,
    "avatar_x_bias": 0.0,
    "avatar_y_bias": -0.001
  },
  "avatar": {
    "state": "calm"
  },
  "ttl": 900
}
```

## Scene Decision bounds

The Android client clamps gateway output before applying it to the renderer:

- `energy`: `0.0 .. 1.0`
- `particle_multiplier`: `0.45 .. 1.8`
- `pulse_multiplier`: `0.6 .. 1.5`
- `scene_scale`: `0.97 .. 1.03`
- avatar X/Y bias: `-0.02 .. 0.02`
- `ttl`: `60 .. 3600` seconds

## Deterministic simulator

`gateway_simulator/aidi_gateway_simulator.py` implements the same `scene-v1` HTTP contract without any external dependencies. It is deterministic for a given request and can run locally:

```bash
python3 gateway_simulator/aidi_gateway_simulator.py --host 127.0.0.1 --port 8765
```

The simulator covers the fast decision states used by the v20 prototype:

| Context | Scene | Avatar |
| --- | --- | --- |
| Battery <= 15%, not charging | `deep_black` | `resting` |
| Night + low motion | `deep_nebula` | `calm` |
| Charging + high motion | `energy_core` | `focused` |
| Main Fold display | `living_continuum` | `aware` |
| Cover/default | `compact_continuum` | `calm` |

Run the protocol suite with:

```bash
python3 gateway_simulator/test_scene_protocol.py
```

The suite verifies health, all five decision paths, response bounds, deterministic decisions, request correlation, malformed JSON rejection, protocol-version rejection, media-type rejection and 404 behavior.

## Safety and autonomy

- Gateway decisions are clamped on-device before they reach the renderer.
- Network work never runs on the wallpaper render thread.
- A decision is cached for its TTL; the app does not call AIDI every frame.
- Connection/read timeouts are bounded.
- If the gateway is unavailable or returns invalid data, `LocalFallbackAI` takes over automatically.
- Existing v9+ personal-universe seed/evolution state is preserved across upgrades.

## Production gateway target

The Android client uses `https://aidi-gateway.hm.dm/api/v1/scene/analyze` as the production AIDI endpoint. The in-repository simulator exposes the same API at `http://127.0.0.1:8765` for deterministic CI protocol tests.
