# v20 Real AIDI Gateway integration

## Verified infrastructure snapshot — 2026-09-23

The production-prototype AIDI path was probed from `ci-jump-gateway.hm.dm`.

- Gateway: `http://192.168.10.214:8088`
- Scene endpoint: `/api/v1/scene/analyze`
- Health endpoint: `/health`
- Protocol: `scene-v1`
- Observed service banner/health version: `AIDI-Gateway/0.21`
- Local LLM endpoint: `http://192.168.10.218:11434/api/chat`
- Observed model: `qwen3-coder:30b`
- Ollama `/api/tags` was reachable from the jump host.
- A real scene request for Fold7 main display returned HTTP 200 and a bounded `living_continuum` decision.

Local DNS name `aidi-gateway.hm.dm` did not resolve from the jump host at the time of verification, therefore the v20 APK defaults to the verified private IPv4 endpoint. The APK build is no longer hard-wired to that address: set `AIDI_GATEWAY_ENDPOINT` as a Gradle property or environment variable to produce an APK for another gateway without editing Java sources.

Example:

```bash
AIDI_GATEWAY_ENDPOINT=https://gateway.example.internal/api/v1/scene/analyze \
  gradle --no-daemon :app:assembleDebug
```

## Deployment bundle

`aidi_gateway/` now contains the service source plus an idempotent installation bundle:

- `server.py` — scene-v1 gateway implementation.
- `aidi-gateway.service` — hardened systemd unit using `DynamicUser`.
- `aidi-gateway.env.example` — infrastructure-specific environment values.
- `install.sh` — installs `/opt/aidi-gateway`, preserves an existing environment file, starts the service, and verifies `/health`.
- `probe_real_gateway.py` — validates health plus representative Fold7 Scene Decision requests.

Install on the target Linux gateway node:

```bash
sudo ./aidi_gateway/install.sh
```

Probe the currently verified gateway from a trusted LAN/VPN node:

```bash
python3 aidi_gateway/probe_real_gateway.py \
  --base-url http://192.168.10.214:8088
```

Use `--require-correlation` after the live gateway has been deployed from this repository; the repository implementation returns both `X-AIDI-Request-Id` and a body `request_id`. The gateway observed on 2026-09-23 accepted correlation input but did not echo it, so the Android client intentionally remains backward-compatible during the migration.

## v20 release gate

v20 is ready to move forward when all of the following are true:

1. Android unit tests and renderer continuity tests pass.
2. Simulator scene-v1 tests pass.
3. The repository `aidi_gateway/server.py` passes a local production-gateway probe with request correlation enabled.
4. APK metadata, INTERNET permission and signature verification pass.
5. A real LAN/VPN gateway probe returns valid bounded Scene Decisions.

The first four gates run in GitHub Actions. Gate 5 is an infrastructure deployment check and is executed from the trusted network rather than from a public GitHub-hosted runner.
