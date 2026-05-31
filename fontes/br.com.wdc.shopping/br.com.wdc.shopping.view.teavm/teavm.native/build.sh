#!/bin/bash
# Build the WDC Shopping app using Tauri for desktop, Android or iOS
# Prerequisites:
#   - Java 21 (set JAVA21_HOME or JAVA_HOME)
#   - Maven
#   - Rust toolchain with cargo tauri CLI: cargo install tauri-cli --version "^2"
#   - For Android: Android SDK (ANDROID_HOME), adb
#   - For iOS: Xcode, xcrun simctl
#
# Usage:
#   ./build.sh desktop                              # build desktop (release)
#   ./build.sh desktop --dev                        # dev mode (uses devUrl)
#   ./build.sh android --deploy                     # build + install on device
#   ./build.sh android --api-url http://192.168.1.8:8080 --deploy
#   ./build.sh ios --deploy                         # build + install on simulator
#   ./build.sh ios --api-url http://localhost:8080 --deploy
#
# Targets:
#   desktop      macOS desktop app (default)
#   android      Android APK (debug, aarch64)
#   ios          iOS Simulator app (debug, aarch64-sim)
#
# Options:
#   --dev        Dev mode (desktop only)
#   --deploy     Deploy to device/simulator after build
#   --api-url    API server URL (default: http://localhost:8080)
#   --sim-name   iOS Simulator name (default: "iPhone 16 Pro")

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/../br.com.wdc.shopping.view.teavm.web"
APPLE_BUILD_DIR="$SCRIPT_DIR/src-tauri/gen/apple/build"

# Resolve Java home
JAVA=${JAVA21_HOME:-$JAVA_HOME}
if [ -z "$JAVA" ]; then
    echo "ERROR: Set JAVA21_HOME or JAVA_HOME to a Java 21 installation"
    exit 1
fi

# Ensure cargo is available
source "$HOME/.cargo/env" 2>/dev/null || true

# Parse arguments
TARGET="${1:-desktop}"
shift 2>/dev/null || true

API_URL=""
DEV_MODE=false
DEPLOY=false
SIM_NAME="iPhone 17 Pro"
BUNDLE_ID="br.com.wdc.shopping.desktop"

for arg in "$@"; do
    case "$arg" in
        --dev) DEV_MODE=true ;;
        --deploy) DEPLOY=true ;;
        --api-url=*) API_URL="${arg#--api-url=}" ;;
        --api-url) shift_next_api=true ;;
        --sim-name=*) SIM_NAME="${arg#--sim-name=}" ;;
        --sim-name) shift_next_sim=true ;;
        *)
            if [ "$shift_next_api" = true ]; then API_URL="$arg"; shift_next_api=false;
            elif [ "$shift_next_sim" = true ]; then SIM_NAME="$arg"; shift_next_sim=false;
            else echo "WARNING: Unknown argument: $arg"; fi
            ;;
    esac
done

# Set default API URL based on target
if [ -z "$API_URL" ]; then
    case "$TARGET" in
        android) API_URL="http://$(ifconfig en0 2>/dev/null | grep 'inet ' | awk '{print $2}'):8080" ;;
        *)       API_URL="http://localhost:8080" ;;
    esac
fi

# Validate target
case "$TARGET" in
    desktop|android|ios) ;;
    *)
        echo "ERROR: Unknown target '$TARGET'. Use: desktop, android, ios"
        exit 1
        ;;
esac

echo "============================================"
echo "  WDC Shopping - Build"
echo "============================================"
echo "  Target:   $TARGET"
echo "  API URL:  $API_URL"
echo "  Deploy:   $DEPLOY"
[ "$TARGET" = "ios" ] && echo "  Sim:      $SIM_NAME"
echo "============================================"
echo ""

# 1. Build the web subproject (Java → JS via TeaVM)
echo ">>> [1/2] Building TeaVM web module..."
cd "$WEB_DIR"
JAVA_HOME="$JAVA" mvn clean install -DskipTests -q -Dapi.base.url="$API_URL"
echo "    OK"
echo ""

# 2. Build the Tauri app for the target platform
echo ">>> [2/2] Building Tauri app ($TARGET)..."
cd "$SCRIPT_DIR"

case "$TARGET" in
    desktop)
        if [ "$DEV_MODE" = true ]; then
            echo "    Starting in dev mode..."
            cargo tauri dev
        else
            cargo tauri build
            echo ""
            echo ">>> Build complete!"
            echo "    App: $SCRIPT_DIR/src-tauri/target/release/bundle/macos/WDC Shopping.app"
        fi
        ;;

    android)
        # Copy custom icons to the generated Android resources
        ANDROID_RES="$SCRIPT_DIR/src-tauri/gen/android/app/src/main/res"
        CUSTOM_ICONS="$SCRIPT_DIR/src-tauri/icons/android"
        if [ -d "$CUSTOM_ICONS" ]; then
            echo "    Copying custom Android icons..."
            for density in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
                if [ -d "$CUSTOM_ICONS/$density" ]; then
                    cp -f "$CUSTOM_ICONS/$density/"* "$ANDROID_RES/$density/" 2>/dev/null || true
                fi
            done
            # Adaptive icon XML + background color
            if [ -d "$CUSTOM_ICONS/mipmap-anydpi-v26" ]; then
                mkdir -p "$ANDROID_RES/mipmap-anydpi-v26"
                cp -f "$CUSTOM_ICONS/mipmap-anydpi-v26/"* "$ANDROID_RES/mipmap-anydpi-v26/"
            fi
            if [ -f "$CUSTOM_ICONS/values/ic_launcher_background.xml" ]; then
                cp -f "$CUSTOM_ICONS/values/ic_launcher_background.xml" "$ANDROID_RES/values/"
            fi
        fi

        cargo tauri android build --target aarch64 --debug 2>&1 | tail -10
        APK_PATH="$SCRIPT_DIR/src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk"
        echo ""
        echo ">>> Build complete!"
        echo "    APK: $APK_PATH"

        if [ "$DEPLOY" = true ]; then
            echo ""
            echo ">>> Deploying to Android device..."
            ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
            "$ADB" install -r "$APK_PATH"
            "$ADB" shell monkey -p "${BUNDLE_ID}.debug" -c android.intent.category.LAUNCHER 1 2>&1
            echo "    App launched on Android"
        fi
        ;;

    ios)
        # Copy custom iOS icons to the Xcode asset catalog
        IOS_ICONS="$SCRIPT_DIR/src-tauri/icons/ios"
        IOS_ASSETS="$SCRIPT_DIR/src-tauri/gen/apple/Assets.xcassets/AppIcon.appiconset"
        if [ -d "$IOS_ICONS" ] && [ -d "$IOS_ASSETS" ]; then
            echo "    Copying custom iOS icons..."
            cp -f "$IOS_ICONS"/*.png "$IOS_ASSETS/"
        fi

        # Clean stale build dir to avoid "Directory not empty" error
        rm -rf "$APPLE_BUILD_DIR" 2>/dev/null || true
        cargo tauri ios build --debug --target aarch64-sim 2>&1 | tail -10

        # Find the .app bundle
        IOS_APP=$(find ~/Library/Developer/Xcode/DerivedData/wdc-shopping-desktop-*/Build/Products/debug-iphonesimulator -name "*.app" -type d 2>/dev/null | head -1)
        if [ -z "$IOS_APP" ]; then
            IOS_APP="$APPLE_BUILD_DIR/arm64-sim/WDC Shopping.app"
        fi
        echo ""
        echo ">>> Build complete!"
        echo "    App: $IOS_APP"

        if [ "$DEPLOY" = true ]; then
            echo ""
            echo ">>> Deploying to iOS Simulator ($SIM_NAME)..."
            xcrun simctl terminate "$SIM_NAME" "$BUNDLE_ID" 2>/dev/null || true
            xcrun simctl install "$SIM_NAME" "$IOS_APP"
            xcrun simctl launch "$SIM_NAME" "$BUNDLE_ID"
            echo "    App launched on $SIM_NAME"
        fi
        ;;
esac

echo ""
echo ">>> Done!"
