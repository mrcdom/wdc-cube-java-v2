#!/usr/bin/env bash
# Writes the backend's configuration from the environment (secrets included: they never go into the image) and starts
# it on the work directory /work.
set -euo pipefail

: "${STORE_JWT_SECRET:?STORE_JWT_SECRET is required}"
: "${STORE_REMOTE_SIGNING_KEY:?STORE_REMOTE_SIGNING_KEY is required: every instance signs the remote session ids with it}"

cors=""
if [ -n "${STORE_CORS_ALLOWED_HOSTS:-}" ]; then
  # other origins whose pages call this API (a page served here needs no entry)
  cors="cors.allowedHosts = \"$STORE_CORS_ALLOWED_HOSTS\""
fi

peers=""
if [ -n "${STORE_CLUSTER_PEERS:-}" ]; then
  # "ws://a:8080,ws://b:8080" → ["ws://a:8080", "ws://b:8080"]
  peers="peers = [$(printf '%s' "$STORE_CLUSTER_PEERS" | sed 's/[^,][^,]*/"&"/g; s/,/, /g')]"
fi

cat > /work/config/application.toml <<TOML
[database]
url = "${STORE_DB_URL:?STORE_DB_URL is required}"
username = "${STORE_DB_USER:-store}"
password = "${STORE_DB_PASSWORD:?STORE_DB_PASSWORD is required}"
transaction = "${STORE_DB_TRANSACTION:-non-jta}"
reset = ${STORE_DB_RESET:-false}

[server]
port = 8080
devMode = false
$cors

[security]
jwt.secret = "$STORE_JWT_SECRET"
auth.requestsPerMinute = ${STORE_AUTH_REQUESTS_PER_MINUTE:-60}

[management]
token = "${STORE_MANAGEMENT_TOKEN:-}"

[remote]
signingKey = "$STORE_REMOTE_SIGNING_KEY"

[cluster]
$peers
TOML

exec java ${JAVA_OPTS:-} -jar /app/backend.jar --workdir=/work
