#!/bin/bash
set -e
cd "$(dirname "$0")"

# Install the dependencies when missing (exactly what package-lock.json locks): this shell's, and those of the
# bridge it takes from its sources (alias @wdc/remote-bridge-react), which resolve from the bridge's folder
BRIDGE_DIR="../../../br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.react"
if [ ! -d "node_modules" ]; then
    npm ci --no-audit --no-fund
fi
if [ ! -d "$BRIDGE_DIR/node_modules" ]; then
    (cd "$BRIDGE_DIR" && npm ci --no-audit --no-fund)
fi

DEPLOY_DIR="../../../../work/frontend/remote.shell.react"

# Build React SPA (output goes to work/frontend/remote.shell.react via distDir in package.json)
# context.html is copied automatically by the build:clean script
npm run build:clean

echo "Deploy complete: $DEPLOY_DIR"
