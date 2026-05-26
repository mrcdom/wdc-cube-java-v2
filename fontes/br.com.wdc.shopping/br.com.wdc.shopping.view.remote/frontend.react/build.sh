#!/bin/bash
set -e
cd "$(dirname "$0")"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    npm install
fi

# Build React SPA (output goes to work/frontend/remote.react via distDir in package.json)
npm run build:clean

echo "Deploy complete: ../../br.com.wdc.shopping.backend/work/frontend/remote.react"
