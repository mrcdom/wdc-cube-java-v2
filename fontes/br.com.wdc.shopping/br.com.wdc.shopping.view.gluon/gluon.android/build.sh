#!/bin/bash
# Build and deploy the WDC Shopping Gluon Android app
#
# Prerequisites:
#   - Java 21 (set JAVA21_HOME or JAVA_HOME)
#   - Maven
#   - Android SDK (ANDROID_HOME) + adb
#   - GluonFX native build REQUIRES Linux x86_64 host (or Docker)
#
# Usage:
#   ./build.sh                  # build JARs only (any platform)
#   ./build.sh --native         # native compile + APK (Linux x86_64 only)
#   ./build.sh --native --deploy  # native build + install + launch on device
#   ./build.sh --deploy-only    # skip build, just install + launch last APK
#
# NOTE: GluonFX cannot cross-compile to Android from macOS ARM.
# On macOS, this script builds the JARs. For APK, use a Linux CI or Docker.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."
BUNDLE_ID="br.com.wdc.shopping"

# Resolve Java home
JAVA=${JAVA21_HOME:-$JAVA_HOME}
if [ -z "$JAVA" ]; then
    echo "ERROR: Set JAVA21_HOME or JAVA_HOME to a Java 21 installation"
    exit 1
fi
export JAVA_HOME="$JAVA"
export PATH="$JAVA_HOME/bin:$PATH"

# Resolve Android SDK
ANDROID=${ANDROID_HOME:-$HOME/Library/Android/sdk}
if [ ! -d "$ANDROID" ]; then
    echo "ERROR: Android SDK not found. Set ANDROID_HOME."
    exit 1
fi
export ANDROID_HOME="$ANDROID"
ADB="$ANDROID/platform-tools/adb"

# Parse arguments
DEPLOY=false
DEPLOY_ONLY=false
NATIVE=false

for arg in "$@"; do
    case "$arg" in
        --deploy)      DEPLOY=true ;;
        --deploy-only) DEPLOY_ONLY=true ;;
        --native)      NATIVE=true ;;
        *) echo "WARNING: Unknown argument: $arg" ;;
    esac
done

echo "============================================"
echo "  WDC Shopping Gluon - Android Build"
echo "============================================"
echo "  JAVA_HOME:    $JAVA_HOME"
echo "  ANDROID_HOME: $ANDROID_HOME"
echo "  Native:       $NATIVE"
echo "  Deploy:       $DEPLOY"
echo "============================================"
echo ""

if [ "$DEPLOY_ONLY" = false ]; then
    # 1. Build dependencies (from parent)
    echo ">>> [1/2] Building project dependencies..."
    cd "$PROJECT_DIR"
    mvn -q -DskipTests install -pl gluon.android -am
    echo "    OK"
    echo ""

    if [ "$NATIVE" = true ]; then
        # Check platform — GluonFX requires Linux x86_64 for Android target
        HOST_OS=$(uname -s)
        HOST_ARCH=$(uname -m)
        if [[ "$HOST_OS" != "Linux" || "$HOST_ARCH" != "x86_64" ]]; then
            echo "ERROR: GluonFX native Android build requires Linux x86_64."
            echo "       Current host: $HOST_OS $HOST_ARCH"
            echo "       Use a Linux CI/CD pipeline or Docker for native builds."
            exit 1
        fi

        echo ">>> [2/2] Native compile + package (GluonFX)..."
        cd "$SCRIPT_DIR"
        mvn -DskipTests gluonfx:compile gluonfx:package
        echo "    OK"
    else
        echo ">>> [2/2] Building project JAR..."
        cd "$SCRIPT_DIR"
        mvn -q -DskipTests package
        echo "    OK"
        echo ""
        echo "    NOTE: JAR built. For APK, run with --native on Linux x86_64."
    fi
    echo ""
fi

# Find APK (only relevant for native builds)
APK=$(find "$SCRIPT_DIR/target/gluonfx" -name "*.apk" -type f 2>/dev/null | head -1)

# Deploy
if [ "$DEPLOY" = true ] || [ "$DEPLOY_ONLY" = true ]; then
    if [ -z "$APK" ]; then
        echo "ERROR: No APK found. Run with --native on Linux x86_64 first."
        exit 1
    fi

    echo ">>> APK: $APK"
    echo ""
    echo ">>> Deploying to Android device..."

    if ! "$ADB" devices | grep -q "device$"; then
        echo "ERROR: No Android device connected. Connect a device or start an emulator."
        exit 1
    fi

    "$ADB" install -r "$APK"
    echo "    Installed."

    # Launch the app
    "$ADB" shell monkey -p "$BUNDLE_ID" -c android.intent.category.LAUNCHER 1 2>&1
    echo "    App launched."
fi

echo ""
echo ">>> Done!"
