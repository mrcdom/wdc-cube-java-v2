#!/bin/bash
# Build script for remote.shell.flutter (Flutter Web / WASM)
#
# Usage:
#   ./build.sh                    — release build (sem source maps)
#   SOURCE_MAPS=true ./build.sh   — release build com source maps para depurar no navegador
#
# O build é feito em modo WASM (--wasm). O diretório de destino é limpo
# completamente antes de cada build para evitar arquivos obsoletos.
#
# Source maps: quando habilitados, arquivos .map são gerados ao lado dos assets
# JS/WASM. O DevTools do navegador os usa automaticamente para exibir o código
# Dart original em breakpoints e stack traces.

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
