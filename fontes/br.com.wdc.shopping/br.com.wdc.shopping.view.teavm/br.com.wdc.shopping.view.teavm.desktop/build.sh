#!/bin/bash
# Build the WDC Shopping Desktop app using Tauri
# Prerequisites:
#   - Java 21 (set JAVA21_HOME or JAVA_HOME)
#   - Maven
#   - Rust toolchain
#   - cargo tauri CLI: cargo install tauri-cli --version "^2"
#
# Usage:
#   JAVA21_HOME=/path/to/java21 ./build.sh
#   JAVA21_HOME=/path/to/java21 ./build.sh --dev    # dev mode (uses devUrl)
#   JAVA21_HOME=/path/to/java21 ./build.sh --api-url http://myserver:9090

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/../br.com.wdc.shopping.view.teavm.web"

# Resolve Java home
JAVA=${JAVA21_HOME:-$JAVA_HOME}
if [ -z "$JAVA" ]; then
    echo "ERROR: Set JAVA21_HOME or JAVA_HOME to a Java 21 installation"
    exit 1
fi

# Parse arguments
API_URL=""
DEV_MODE=false
for arg in "$@"; do
    case "$arg" in
        --dev) DEV_MODE=true ;;
        --api-url=*) API_URL="${arg#--api-url=}" ;;
        --api-url) shift_next=true ;;
        *) if [ "$shift_next" = true ]; then API_URL="$arg"; shift_next=false; fi ;;
    esac
done

MVN_ARGS="-DskipTests -q"
if [ -n "$API_URL" ]; then
    MVN_ARGS="$MVN_ARGS -Dapi.base.url=$API_URL"
    echo "    API URL: $API_URL"
fi

# 1. Build the web subproject (Java → JS via TeaVM, package as JAR)
echo ">>> Building TeaVM web subproject..."
cd "$WEB_DIR"
JAVA_HOME="$JAVA" mvn clean install $MVN_ARGS
echo "    Web build OK"

# 2. Build the Tauri desktop app
echo ">>> Building Tauri desktop app..."
cd "$SCRIPT_DIR"

if [ "$DEV_MODE" = true ]; then
    echo "    Starting in dev mode..."
    cargo tauri dev
else
    cargo tauri build
    echo ""
    echo ">>> Build complete!"
    echo "    App: $SCRIPT_DIR/src-tauri/target/release/bundle/macos/WDC Shopping.app"
    echo "    DMG: $SCRIPT_DIR/src-tauri/target/release/bundle/dmg/"
fi
