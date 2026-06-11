#!/bin/bash
# Build and run the WDC Shopping Gluon Desktop app
#
# Prerequisites:
#   - Java 21 (set JAVA21_HOME or JAVA_HOME)
#   - Maven
#   - For native: GraalVM with native-image (GRAALVM_HOME)
#
# Usage:
#   ./build.sh                  # build + run via JavaFX plugin
#   ./build.sh --run            # same as above (default)
#   ./build.sh --build-only     # build JARs without running
#   ./build.sh --native         # native compile via GluonFX (host target)
#   ./build.sh --native --run   # native compile + run native binary
#
# Options:
#   --run          Run the app after build (default behavior)
#   --build-only   Build without running
#   --native       Use GluonFX native-image instead of JVM
#   --api-url=X    API server URL (default: http://localhost:8080)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."

# Resolve Java home
JAVA=${JAVA21_HOME:-$JAVA_HOME}
if [ -z "$JAVA" ]; then
    echo "ERROR: Set JAVA21_HOME or JAVA_HOME to a Java 21 installation"
    exit 1
fi
export JAVA_HOME="$JAVA"
export PATH="$JAVA_HOME/bin:$PATH"

# Parse arguments
RUN=true
NATIVE=false
API_URL="http://localhost:8080"

for arg in "$@"; do
    case "$arg" in
        --run)        RUN=true ;;
        --build-only) RUN=false ;;
        --native)     NATIVE=true ;;
        --api-url=*)  API_URL="${arg#--api-url=}" ;;
        *) echo "WARNING: Unknown argument: $arg" ;;
    esac
done

# For native builds, switch to GraalVM
if [ "$NATIVE" = true ]; then
    if [ -z "$GRAALVM_HOME" ]; then
        echo "ERROR: Set GRAALVM_HOME for native compilation."
        exit 1
    fi
    export JAVA_HOME="$GRAALVM_HOME"
    export PATH="$GRAALVM_HOME/bin:$PATH"
fi

echo "============================================"
echo "  WDC Shopping Gluon - Desktop Build"
echo "============================================"
echo "  JAVA_HOME:  $JAVA_HOME"
echo "  Native:     $NATIVE"
echo "  Run:        $RUN"
echo "  API URL:    $API_URL"
echo "============================================"
echo ""

# 1. Build dependencies
echo ">>> [1/2] Building project dependencies..."
cd "$PROJECT_DIR"
mvn -q -DskipTests install -pl gluon.desktop -am
echo "    OK"
echo ""

if [ "$NATIVE" = true ]; then
    echo ">>> [2/2] Native compile (GluonFX, target=host)..."
    cd "$SCRIPT_DIR"
    mvn -DskipTests gluonfx:compile gluonfx:link
    echo "    OK"
    echo ""

    if [ "$RUN" = true ]; then
        echo ">>> Running native binary..."
        mvn -DskipTests gluonfx:nativerun
    fi
else
    if [ "$RUN" = true ]; then
        echo ">>> [2/2] Running via JavaFX plugin..."
        cd "$SCRIPT_DIR"
        mvn javafx:run -Dapi.base.url="$API_URL" -Dshopping.config.file="$SCRIPT_DIR/work/config/application.toml"
    else
        echo ">>> [2/2] Building project JAR..."
        cd "$SCRIPT_DIR"
        mvn -q -DskipTests package
        echo "    OK"
    fi
fi

echo ""
echo ">>> Done!"
