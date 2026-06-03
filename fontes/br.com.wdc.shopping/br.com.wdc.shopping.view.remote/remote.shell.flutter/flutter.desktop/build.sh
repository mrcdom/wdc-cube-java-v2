#!/bin/bash
set -e
cd "$(dirname "$0")"

case "$(uname -s)" in
  Darwin*)  PLATFORM=macos ;;
  Linux*)   PLATFORM=linux ;;
  MINGW*|MSYS*|CYGWIN*) PLATFORM=windows ;;
  *) echo "OS not supported: $(uname -s)"; exit 1 ;;
esac

ENDPOINT="${WDC_ENDPOINT:-http://localhost:8080}"

echo "Building Flutter desktop ($PLATFORM) -> $ENDPOINT"
flutter build $PLATFORM --release --dart-define=WDC_ENDPOINT=$ENDPOINT

echo "Build complete."
