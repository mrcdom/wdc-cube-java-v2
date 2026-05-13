#!/bin/bash
# Build and deploy the WDC Shopping Gluon iOS app
#
# Prerequisites:
#   - macOS with Xcode installed
#   - Java 21 (set JAVA21_HOME or JAVA_HOME) for JAR compilation
#   - GraalVM Gluon (GRAALVM_HOME) for native compilation
#   - Maven
#   - xcrun simctl (for simulator deploy)
#
# Usage:
#   ./build.sh                              # build JARs only
#   ./build.sh --native                     # native compile for iOS device
#   ./build.sh --native --sim              # native compile for iOS simulator
#   ./build.sh --native --sim --deploy     # build + install + launch on simulator
#   ./build.sh --native --deploy           # build + install on device
#   ./build.sh --deploy-only              # install last .app on simulator
#
# Options:
#   --native       Run GluonFX native compile + package (requires GRAALVM_HOME)
#   --sim          Target iOS Simulator instead of physical device
#   --deploy       Deploy after build
#   --deploy-only  Skip build, just install + launch on simulator
#   --sim-name=X   Simulator name (default: "iPhone 16 Pro")
#
# KNOWN ISSUE: GraalVM Gluon CE 23 (Substrate 0.0.68) has a CAP cache
# incompatible with Xcode 26+ (struct___darwin_mcontext64 changed).
# Native builds fail with "Missing CAP cache value". This requires an
# updated Substrate version or GraalVM Gluon build.

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

# Parse arguments
DEPLOY=false
DEPLOY_ONLY=false
NATIVE=false
SIM=false
SIM_NAME="iPhone 16 Pro"

for arg in "$@"; do
    case "$arg" in
        --deploy)      DEPLOY=true ;;
        --deploy-only) DEPLOY_ONLY=true ;;
        --native)      NATIVE=true ;;
        --sim)         SIM=true ;;
        --sim-name=*)  SIM_NAME="${arg#--sim-name=}" ;;
        *) echo "WARNING: Unknown argument: $arg" ;;
    esac
done

# For native builds, switch JAVA_HOME to GraalVM
if [ "$NATIVE" = true ]; then
    if [ -z "$GRAALVM_HOME" ]; then
        echo "ERROR: Set GRAALVM_HOME for native compilation."
        echo "       Found: /Library/Java/JavaVirtualMachines/graalvm-gluon-23.jdk/Contents/Home"
        exit 1
    fi
    export JAVA_HOME="$GRAALVM_HOME"
    export PATH="$GRAALVM_HOME/bin:$PATH"
fi

# Determine target label
if [ "$SIM" = true ]; then
    TARGET_LABEL="ios-sim (simulator)"
else
    TARGET_LABEL="ios (device)"
fi

echo "============================================"
echo "  WDC Shopping Gluon - iOS Build"
echo "============================================"
echo "  JAVA_HOME:    $JAVA_HOME"
echo "  Native:       $NATIVE"
echo "  Target:       $TARGET_LABEL"
echo "  Deploy:       $DEPLOY"
[ "$SIM" = true ] && echo "  Simulator:    $SIM_NAME"
echo "============================================"
echo ""

if [ "$DEPLOY_ONLY" = false ]; then
    # 1. Build dependencies
    echo ">>> [1/2] Building project dependencies..."
    cd "$PROJECT_DIR"
    mvn -q -DskipTests install -pl gluon.ios -am
    echo "    OK"
    echo ""

    if [ "$NATIVE" = true ]; then
        # Verify macOS
        if [[ "$(uname -s)" != "Darwin" ]]; then
            echo "ERROR: iOS builds require macOS with Xcode."
            exit 1
        fi

        PROFILE_ARG=""
        if [ "$SIM" = true ]; then
            PROFILE_ARG="-Pios-sim"
        fi

        echo ">>> [2/2] Native compile + package (GluonFX)..."
        cd "$SCRIPT_DIR"
        mvn -DskipTests gluonfx:compile gluonfx:link gluonfx:package $PROFILE_ARG
        echo "    OK"
    else
        echo ">>> [2/2] Building project JAR..."
        cd "$SCRIPT_DIR"
        mvn -q -DskipTests package
        echo "    OK"
        echo ""
        echo "    NOTE: JAR built. For native iOS app, run with --native."
    fi
    echo ""
fi

# Deploy
if [ "$DEPLOY" = true ] || [ "$DEPLOY_ONLY" = true ]; then
    # Find the .app
    IOS_APP=$(find "$SCRIPT_DIR/target/gluonfx" -name "*.app" -type d 2>/dev/null | head -1)

    if [ -z "$IOS_APP" ]; then
        echo "ERROR: No .app found in target/gluonfx/"
        echo "       Run with --native first to build the native app."
        exit 1
    fi

    echo ">>> App: $IOS_APP"

    if [ "$SIM" = true ] || [ "$DEPLOY_ONLY" = true ]; then
        echo ""
        echo ">>> Deploying to iOS Simulator ($SIM_NAME)..."
        xcrun simctl boot "$SIM_NAME" 2>/dev/null || true
        xcrun simctl terminate "$SIM_NAME" "$BUNDLE_ID" 2>/dev/null || true
        xcrun simctl install "$SIM_NAME" "$IOS_APP"
        xcrun simctl launch "$SIM_NAME" "$BUNDLE_ID"
        echo "    App launched on $SIM_NAME"
    else
        echo ""
        echo ">>> Installing on device via GluonFX..."
        cd "$SCRIPT_DIR"
        mvn -DskipTests gluonfx:install
        echo "    App installed on device."
    fi
fi

echo ""
echo ">>> Done!"
