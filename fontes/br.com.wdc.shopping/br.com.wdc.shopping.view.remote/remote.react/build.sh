#!/bin/bash
set -e
cd "$(dirname "$0")"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    npm install
fi

DEPLOY_DIR="../../../../work/frontend/remote.react"

# Build React SPA (output goes to work/frontend/remote.react via distDir in package.json)
# context.html is copied automatically by the build:clean script
npm run build:clean

echo "Deploy complete: $DEPLOY_DIR"
