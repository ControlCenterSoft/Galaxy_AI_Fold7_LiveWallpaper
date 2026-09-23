#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${AIDI_INSTALL_DIR:-/opt/aidi-gateway}"
ENV_DIR="${AIDI_ENV_DIR:-/etc/aidi-gateway}"
SERVICE_PATH="/etc/systemd/system/aidi-gateway.service"

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
  echo "Run as root (or via sudo)." >&2
  exit 1
fi

install -d -m 0755 "$INSTALL_DIR" "$ENV_DIR"
install -m 0755 "$SCRIPT_DIR/server.py" "$INSTALL_DIR/server.py"
install -m 0644 "$SCRIPT_DIR/memory.py" "$INSTALL_DIR/memory.py"
install -m 0644 "$SCRIPT_DIR/aidi-gateway.service" "$SERVICE_PATH"

if [[ ! -f "$ENV_DIR/aidi-gateway.env" ]]; then
  install -m 0640 "$SCRIPT_DIR/aidi-gateway.env.example" "$ENV_DIR/aidi-gateway.env"
  echo "Created $ENV_DIR/aidi-gateway.env from the example configuration."
else
  echo "Preserved existing $ENV_DIR/aidi-gateway.env."
fi

systemctl daemon-reload
systemctl enable --now aidi-gateway.service

PORT="$(awk -F= '$1=="AIDI_GATEWAY_PORT" {print $2}' "$ENV_DIR/aidi-gateway.env" | tail -1)"
PORT="${PORT:-8088}"

for _ in $(seq 1 30); do
  if curl -fsS --max-time 2 "http://127.0.0.1:${PORT}/health" >/tmp/aidi-gateway-health.json; then
    cat /tmp/aidi-gateway-health.json
    echo
    echo "AIDI Gateway v21 is healthy on port ${PORT}."
    exit 0
  fi
  sleep 1
done

systemctl --no-pager --full status aidi-gateway.service || true
journalctl -u aidi-gateway.service -n 80 --no-pager || true
echo "AIDI Gateway failed the post-install health check." >&2
exit 1
