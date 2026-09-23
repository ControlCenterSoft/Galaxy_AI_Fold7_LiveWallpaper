# v21 Gateway hardening baseline

v21 AI Memory Universe inherits the real-infrastructure integration baseline validated by v20 rather than regressing to a hard-coded or single-retry client.

## Inherited production properties

- APK endpoint is build-configurable through `AIDI_GATEWAY_ENDPOINT` while retaining the verified LAN default `http://192.168.10.214:8088/api/v1/scene/analyze`.
- Android keeps `profile-v1` memory enrichment and now also retains progressive Gateway recovery: 60, 120, 240, 480, then at most 900 seconds.
- Gateway failure immediately uses `LocalFallbackAI`; renderer operation does not wait for network recovery.
- Successful remote recovery resets the failure counter and returns the decision source to `aidi_gateway`.
- Null Gateway decisions and executor shutdown races are treated safely.
- The Linux deployment bundle installs both `server.py` and the v21 `memory.py` module.
- The systemd unit uses a dynamic service identity and an external environment file rather than embedding infrastructure configuration in the unit.
- Optional Qdrant settings remain environment-only; no API key is committed.

## Verified infrastructure baseline

On 2026-09-23 the trusted jump host reached:

- AIDI Gateway `192.168.10.214:8088`, protocol `scene-v1`, observed version `0.21`.
- Local Ollama `192.168.10.218:11434`, with `qwen3-coder:30b` selected by the Gateway.
- Representative Fold7 scene requests returned valid bounded Scene Decisions.

The live service accepted request correlation input but did not echo it during the migration probe. Repository v21 does echo correlation and CI requires this behavior from the staged implementation. This permits a controlled live upgrade without making the Android client incompatible with the currently running Gateway.

## Deployment

```bash
sudo ./aidi_gateway/install.sh
python3 aidi_gateway/probe_real_gateway.py --base-url http://127.0.0.1:8088 --require-correlation
```

For persistent memory, configure `AIDI_QDRANT_URL`, collection and optional API key in `/etc/aidi-gateway/aidi-gateway.env` before restarting the service.

## Release hand-off

The v21 CI gate now validates memory, endpoint injection, progressive retry, deployment assets, a locally staged production Gateway with request correlation, Android compilation, APK metadata/signature and artifact upload. A live LAN/VPN probe remains the final infrastructure acceptance step after deployment.
