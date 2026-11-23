#!/bin/bash
# Build script for remote.shell.flutter (Flutter Web / WASM)
#
# Usage:
#   ./build.sh                    — release build (no source maps)
#   SOURCE_MAPS=true ./build.sh   — release build with source maps for debugging in the browser
#
# The build runs in WASM mode (--wasm). The target directory is wiped
# completely before each build to avoid stale files.
#
# Source maps: when enabled, .map files are generated next to the JS/WASM
# assets. The browser DevTools use them automatically to show the original
# Dart code in breakpoints and stack traces.

set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../../../../work/frontend/remote.shell.flutter"

# Source maps: enable with SOURCE_MAPS=true ./build.sh
SOURCE_MAPS_FLAG=""
if [ "${SOURCE_MAPS}" = "true" ]; then
  SOURCE_MAPS_FLAG="--source-maps"
  echo "Source maps: ENABLED"
else
  echo "Source maps: disabled (use SOURCE_MAPS=true ./build.sh to enable)"
fi

echo "Cleaning deploy directory..."
rm -rf "$DEPLOY_DIR"

echo "Building Flutter web..."
flutter build web --wasm --base-href="/remote.shell.flutter/" --output="$DEPLOY_DIR" --release $SOURCE_MAPS_FLAG

echo "Copying context.html..."
cp "$(dirname "$0")/context.html" "$DEPLOY_DIR/context.html"

echo "Deploy complete: $DEPLOY_DIR"
