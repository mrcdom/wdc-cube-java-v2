#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "Watching for changes in src/ ..."
echo "Press Ctrl+C to stop."

# Initial build
./build.sh

# Watch for changes and rebuild (exclude generated css)
fswatch -o --exclude='\.css$' src/ | while read; do
    echo ""
    echo "=== Change detected, rebuilding... ==="
    ./build.sh || echo "BUILD FAILED"
done
