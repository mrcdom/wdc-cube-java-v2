#!/usr/bin/env bash
set -euo pipefail

# ── Build and deploy to iPhone Simulator ────────────────────────────
# Usage:
#   ./run-sim.sh              # clean build + deploy + launch
#   ./run-sim.sh --no-clean   # incremental build (skip clean)
#   ./run-sim.sh --deps       # rebuild dependencies first, then build + deploy

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

CLEAN=true
BUILD_DEPS=false

for arg in "$@"; do
    case "$arg" in
        --no-clean) CLEAN=false ;;
        --deps)     BUILD_DEPS=true ;;
        -h|--help)
            echo "Usage: $0 [--no-clean] [--deps]"
            echo "  --no-clean   Skip 'rm -rf target' (incremental build)"
            echo "  --deps       Rebuild shared dependencies before building"
            exit 0
            ;;
        *) echo "Unknown option: $arg"; exit 1 ;;
    esac
done

# ── Rebuild dependencies if requested ───────────────────────────────
if $BUILD_DEPS; then
    echo "==> Rebuilding robovm-compat dependencies..."
    (cd "$SCRIPT_DIR/../../" && bash build-robovm-deps.sh)
    echo ""
fi

# ── Clean ───────────────────────────────────────────────────────────
if $CLEAN; then
    echo "==> Cleaning target..."
    rm -rf target
fi

# ── Build + Deploy + Launch ─────────────────────────────────────────
echo "==> Building and deploying to iPhone Simulator..."
mvn robovm:iphone-sim -Probovm-compat

echo ""
echo "==> Done. App launched on simulator."
