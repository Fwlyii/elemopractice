#!/bin/zsh

set -e

SCRIPT_DIR="${0:A:h}"
PROJECT_DIR="${SCRIPT_DIR:h}"
ENV_FILE="$PROJECT_DIR/.env"

if [[ -f "$ENV_FILE" ]] && grep -Eq '^JWT_SECRET=.{64,}$' "$ENV_FILE"; then
  exit 0
fi

if ! command -v openssl >/dev/null 2>&1; then
  print "缺少 openssl，无法安全生成 JWT_SECRET。"
  exit 1
fi

umask 077
JWT_VALUE="$(openssl rand -hex 64)"

if [[ -f "$ENV_FILE" ]]; then
  TEMP_FILE="$(mktemp "${TMPDIR:-/tmp}/elm-demo-env.XXXXXX")"
  grep -v '^JWT_SECRET=' "$ENV_FILE" > "$TEMP_FILE" || true
  print -r -- "JWT_SECRET=$JWT_VALUE" >> "$TEMP_FILE"
  mv "$TEMP_FILE" "$ENV_FILE"
else
  print -r -- "JWT_SECRET=$JWT_VALUE" > "$ENV_FILE"
fi

chmod 600 "$ENV_FILE"
print "已在本机生成随机 JWT 签名密钥。"
