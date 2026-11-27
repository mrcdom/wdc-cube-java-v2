#!/usr/bin/env bash
#
# Builds what the image takes and stages it in build/context, the part of the Docker build context the Dockerfile
# copies: the backend's fat JAR, the web frontends of work/frontend and the web cache's own bundle.
#
# Usage:
#   ./prepare.sh                    # mvn clean install (Java 25), then every web frontend, then stage
#   ./prepare.sh --skip-frontends   # builds the backend; takes work/frontend as it is
#   ./prepare.sh --skip-build       # builds nothing; takes the JAR and work/frontend as they are
#
# Maven reads extra arguments from MAVEN_ARGS (e.g. MAVEN_ARGS=-o for an offline build).
set -euo pipefail

DOCKER_DIR="$(cd "$(dirname "$0")" && pwd)"
FONTES_DIR="$(cd "$DOCKER_DIR/../.." && pwd)"
ROOT_DIR="$(cd "$FONTES_DIR/.." && pwd)"
JAR="$FONTES_DIR/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar"

BUILD_BACKEND=true
BUILD_FRONTENDS=true
for arg in "$@"; do
  case "$arg" in
    --skip-build) BUILD_BACKEND=false; BUILD_FRONTENDS=false ;;
    --skip-frontends) BUILD_FRONTENDS=false ;;
    *) echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

if $BUILD_BACKEND; then
  export JAVA_HOME="${JAVA25_HOME:-${JAVA_HOME:-}}"
  (cd "$FONTES_DIR" && mvn -q clean install -DskipTests)
fi
if $BUILD_FRONTENDS; then
  # the modules were just installed: the TeaVM targets compile against them
  "$ROOT_DIR/work/bin/build-frontends.sh" --skip-install
fi

[ -f "$JAR" ] || { echo "Backend JAR not found: $JAR (run without --skip-build)" >&2; exit 1; }
[ -d "$ROOT_DIR/work/frontend" ] || { echo "work/frontend not found (run without --skip-build)" >&2; exit 1; }

CONTEXT="$DOCKER_DIR/build/context"
rm -rf "$CONTEXT"
mkdir -p "$CONTEXT"
cp "$JAR" "$CONTEXT/backend.jar"
cp -R "$ROOT_DIR/work/frontend" "$CONTEXT/frontend"
# the web cache's own bundle of the Spectrum web components, made by a local build (teavm.web loads it); the rest of
# the web cache is fetched from its CDNs on first use
mkdir -p "$CONTEXT/web-cache"
if [ -d "$ROOT_DIR/work/data/web-cache/swc-bundle" ]; then
  cp -RL "$ROOT_DIR/work/data/web-cache/swc-bundle" "$CONTEXT/web-cache/swc-bundle"
else
  echo "Warning: work/data/web-cache/swc-bundle not found: teavm.web will miss its components" >&2
fi
echo "Docker build context ready: $CONTEXT"
