#!/usr/bin/env sh
set -eu

DEFAULT_SERVER="${CRAFTSOCKETPROXY_SERVER:-minecraft.farlongmc.com}"
DEFAULT_PORT="${CRAFTSOCKETPROXY_PORT:-80}"
LOCAL_PORT="${CRAFTSOCKETPROXY_LOCAL_PORT:-25565}"
IMAGE="${CRAFTSOCKETPROXY_IMAGE:-ghcr.io/whatnick/craftsocketproxy:1.0.1-auth}"
FALLBACK_IMAGE="${CRAFTSOCKETPROXY_FALLBACK_IMAGE:-craftsocketproxy-client:local}"
CONTAINER_NAME="${CRAFTSOCKETPROXY_CONTAINER:-craftsocketproxy-client}"

printf 'Minecraft server [%s]: ' "$DEFAULT_SERVER"
read -r SERVER
SERVER=${SERVER:-$DEFAULT_SERVER}

case "$SERVER" in
  *:*)
    SERVER_PORT=${SERVER##*:}
    SERVER_HOST=${SERVER%:*}
    ;;
  *)
    SERVER_HOST=$SERVER
    SERVER_PORT=$DEFAULT_PORT
    ;;
esac

printf 'Server password: '
stty -echo 2>/dev/null || true
read -r SERVER_PASSWORD
stty echo 2>/dev/null || true
printf '\n'

if [ -z "$SERVER_PASSWORD" ]; then
  echo 'Password is required.' >&2
  exit 1
fi

start_with_docker() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  if ! docker run -d --rm \
    --name "$CONTAINER_NAME" \
    -p "127.0.0.1:${LOCAL_PORT}:${LOCAL_PORT}" \
    -e CRAFTSOCKETPROXY_PASSWORD="$SERVER_PASSWORD" \
    "$IMAGE" \
    --c -host "$SERVER_HOST" -port "$SERVER_PORT" -proxy "$LOCAL_PORT" >/dev/null; then
    echo "Could not start $IMAGE. Building from the public fork instead..."
    docker build -t "$FALLBACK_IMAGE" "https://github.com/whatnick/craftsocketproxy.git#master"
    docker run -d --rm \
      --name "$CONTAINER_NAME" \
      -p "127.0.0.1:${LOCAL_PORT}:${LOCAL_PORT}" \
      -e CRAFTSOCKETPROXY_PASSWORD="$SERVER_PASSWORD" \
      "$FALLBACK_IMAGE" \
      --c -host "$SERVER_HOST" -port "$SERVER_PORT" -proxy "$LOCAL_PORT" >/dev/null
  fi
}

start_with_java() {
  if [ -z "${CRAFTSOCKETPROXY_JAR:-}" ]; then
    echo 'Docker was not found. Set CRAFTSOCKETPROXY_JAR=/path/to/CraftSocketProxy-1.0.1-auth.jar to run with Java.' >&2
    exit 1
  fi
  java -jar "$CRAFTSOCKETPROXY_JAR" --c -host "$SERVER_HOST" -port "$SERVER_PORT" -proxy "$LOCAL_PORT" -password "$SERVER_PASSWORD" >/tmp/craftsocketproxy-client.log 2>&1 &
}

if command -v docker >/dev/null 2>&1; then
  start_with_docker
elif command -v java >/dev/null 2>&1; then
  start_with_java
else
  echo 'Install Docker Desktop or Java before running this script.' >&2
  exit 1
fi

unset SERVER_PASSWORD

echo "Local proxy is starting on localhost:${LOCAL_PORT}."
echo "Open Minecraft Java Edition and connect to localhost:${LOCAL_PORT}."

sleep 3

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  docker logs --tail 20 "$CONTAINER_NAME" || true
fi

case "$(uname -s 2>/dev/null || echo unknown)" in
  Darwin*)
    open -a 'Minecraft' >/dev/null 2>&1 || open 'minecraft://' >/dev/null 2>&1 || true
    ;;
  Linux*)
    if command -v minecraft-launcher >/dev/null 2>&1; then
      minecraft-launcher >/dev/null 2>&1 &
    elif command -v xdg-open >/dev/null 2>&1; then
      xdg-open 'minecraft://' >/dev/null 2>&1 || true
    fi
    ;;
  MINGW*|MSYS*|CYGWIN*)
    cmd.exe /c start "" "minecraft:" >/dev/null 2>&1 || true
    ;;
esac
