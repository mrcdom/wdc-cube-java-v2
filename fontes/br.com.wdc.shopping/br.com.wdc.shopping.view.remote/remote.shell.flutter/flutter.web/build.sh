#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../../../../work/frontend/remote.shell.flutter"

echo "Building Flutter web..."
flutter build web --base-href="/remote.shell.flutter/" --output="$DEPLOY_DIR" --release

echo "Deploy complete: $DEPLOY_DIR"
