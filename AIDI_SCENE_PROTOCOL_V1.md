# AIDI Scene Protocol v1

Galaxy AI Fold7 LiveWallpaper v20 communicates with AIDI Gateway through a small, cache-friendly decision protocol. Rendering always stays local; the gateway only changes bounded scene parameters.

## Endpoint

`POST /api/v1/scene/analyze`

Android sends header `X-AIDI-Protocol: scene-v1`.

## Request

```json
{
  "device": "GalaxyFold7",
  "app_version": "20.0",
  "state": {
    "battery": 82,
    "charging": false,
    "fold_state": "main",
    "hour": 22,
    "motion_level": 0.18
  }
}
```

## Response

```json
{
  "decision_id": "aidi-001245",
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

## Safety and autonomy

- Gateway decisions are clamped on-device before they reach the renderer.
- Network work never runs on the wallpaper render thread.
- A decision is cached for its TTL; the app does not call AIDI every frame.
- Connection/read timeouts are bounded.
- If the gateway is unavailable or returns invalid data, `LocalFallbackAI` takes over automatically.
- Existing v9+ personal-universe seed/evolution state is preserved across upgrades.

## Production gateway target

The Android client uses `https://aidi-gateway.hm.dm/api/v1/scene/analyze` as the production AIDI endpoint. The in-repository simulator exposes the same API at `http://127.0.0.1:8765` for CI protocol tests.
