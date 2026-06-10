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
#   ./build.sh desktop                                       # build desktop (release)
#   ./build.sh desktop --dev                                 # dev mode (uses devUrl)
#   ./build.sh android --deploy                              # build + deploy to phone emulator
#   ./build.sh android --deploy --form-factor tablet         # build + deploy to tablet emulator
#   ./build.sh android --api-url http://192.168.1.8:8080 --deploy
#   ./build.sh ios --deploy                                  # build + deploy to iPhone simulator
#   ./build.sh ios --deploy --form-factor tablet             # build + deploy to iPad simulator
#   ./build.sh ios --api-url http://localhost:8080 --deploy
#
# Targets:
#   desktop      macOS desktop app (default)
#   android      Android APK (debug, aarch64)
#   ios          iOS Simulator app (debug, aarch64-sim)
#
# Options:
#   --dev              Dev mode (desktop only)
#   --deploy           Deploy to device/simulator after build
#   --api-url          API server URL (default: http://localhost:8080)
#   --form-factor      phone (default) or tablet — selects the running emulator/simulator

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/../teavm.web"
APPLE_BUILD_DIR="$SCRIPT_DIR/src-tauri/gen/apple/build"
WORK_DIR="$SCRIPT_DIR/../../../../work"
WEB_CACHE_SRC="$WORK_DIR/data/web-cache"
WEB_CACHE_DST="$WORK_DIR/frontend/teavm.web/web-cache"

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
FORM_FACTOR="phone"
BUNDLE_ID="br.com.wdc.shopping.desktop"

for arg in "$@"; do
    case "$arg" in
        --dev) DEV_MODE=true ;;
        --deploy) DEPLOY=true ;;
        --api-url=*) API_URL="${arg#--api-url=}" ;;
        --api-url) shift_next_api=true ;;
        --form-factor=*) FORM_FACTOR="${arg#--form-factor=}" ;;
        --form-factor) shift_next_ff=true ;;
        *)
            if [ "$shift_next_api" = true ]; then API_URL="$arg"; shift_next_api=false;
            elif [ "$shift_next_ff" = true ]; then FORM_FACTOR="$arg"; shift_next_ff=false;
            else echo "WARNING: Unknown argument: $arg"; fi
            ;;
    esac
done

# Set default API URL based on target
if [ -z "$API_URL" ]; then
    case "$TARGET" in
        # Android emulator: 10.0.2.2 is the special alias for the host machine.
        # Physical devices on the same LAN need the host's LAN IP instead (use --api-url).
        android) API_URL="http://10.0.2.2:8080" ;;
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
echo "  Target:       $TARGET"
echo "  API URL:      $API_URL"
echo "  Deploy:       $DEPLOY"
echo "  Form factor:  $FORM_FACTOR"
echo "============================================"
echo ""

# 1. Build the web subproject (Java → JS via TeaVM)
echo ">>> [1/3] Building TeaVM web module..."
cd "$WEB_DIR"
JAVA_HOME="$JAVA" mvn clean install -DskipTests -q -Dapi.base.url="$API_URL"
echo "    OK"
echo ""

# 2. Bundle web-cache into frontendDist (CDN resources: Bootstrap Icons, SWC bundle, fonts)
#    On desktop these are served by the backend; on mobile they must be embedded in the app.
#
#    We copy only the directories needed by index.html:
#      swc-bundle/      — self-contained esbuild bundle of @spectrum-web-components (no /npm:... imports)
#      cdn.jsdelivr.net/npm/bootstrap-icons/ — @-free symlink → bootstrap-icons@1.11.3
#      fonts.googleapis.com/ — Google Fonts CSS + subset
#    The jspm.dev/ directory (hundreds of files with /npm:... absolute imports) is NOT copied —
#    the self-contained bundle replaces it entirely.
echo ">>> [2/3] Bundling web-cache assets..."
if [ -d "$WEB_CACHE_SRC" ]; then
    rm -rf "$WEB_CACHE_DST"
    mkdir -p "$WEB_CACHE_DST"
    # Copy only the required subdirectories (avoids the jspm.dev mess)
    for subdir in swc-bundle cdn.jsdelivr.net fonts.googleapis.com; do
        if [ -d "$WEB_CACHE_SRC/$subdir" ]; then
            rsync -rL "$WEB_CACHE_SRC/$subdir/" "$WEB_CACHE_DST/$subdir/"
        fi
    done
    echo "    OK ($(du -sh "$WEB_CACHE_DST" | cut -f1) copied)"
else
    echo "    WARNING: web-cache not found at $WEB_CACHE_SRC — icons and fonts may not render"
fi
echo ""

# 3. Build the Tauri app for the target platform
# Patch api-base-url immediately before cargo embeds the files.
# Done here (not in step 1) so it survives any IDE mvn rebuild that may happen between steps.
# The `touch` forces cargo to detect the change and re-embed (avoids stale incremental cache).
FRONTEND_INDEX="$WORK_DIR/frontend/teavm.web/index.html"
if [ -f "$FRONTEND_INDEX" ]; then
    sed -i '' "s|<meta name=\"api-base-url\" content=\"[^\"]*\">|<meta name=\"api-base-url\" content=\"$API_URL\">|" "$FRONTEND_INDEX"
    touch "$FRONTEND_INDEX"
fi

echo ">>> [3/3] Building Tauri app ($TARGET)..."
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
            ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

            # Auto-detect the first running emulator matching the requested form factor.
            # Uses ro.build.characteristics (contains "tablet" for tablets) — more reliable than ro.product.model.
            ADB_DEVICE=""
            while IFS= read -r serial; do
                characteristics=$("$ADB" -s "$serial" shell getprop ro.build.characteristics </dev/null 2>/dev/null | tr -d '\r\n') || true
                [ -z "$characteristics" ] && continue
                is_tablet=false
                echo "$characteristics" | grep -qi "tablet" && is_tablet=true || true
                if [ "$FORM_FACTOR" = "tablet" ] && [ "$is_tablet" = true ]; then
                    ADB_DEVICE="$serial"; break
                elif [ "$FORM_FACTOR" != "tablet" ] && [ "$is_tablet" = false ]; then
                    ADB_DEVICE="$serial"; break
                fi
            done < <("$ADB" devices | awk '/\tdevice$/{print $1}')

            if [ -z "$ADB_DEVICE" ]; then
                echo "ERROR: No running Android emulator found for form-factor '$FORM_FACTOR'."
                echo "       Start the appropriate AVD and retry."
                exit 1
            fi

            echo ">>> Deploying to Android emulator: $ADB_DEVICE (form-factor: $FORM_FACTOR)..."
            # Uninstall ALL known WDC apps to free storage before install.
            # A 230MB debug APK requires ~1GB free; emulator storage fills up quickly.
            echo "    Freeing storage (uninstalling WDC apps + trimming caches)..."
            for pkg in \
                br.com.wdc.shopping.desktop.debug \
                br.com.wdc.shopping.android \
                br.com.wdc.shopping.nativeui.android \
                br.com.wdc.flutter_mobile; do
                "$ADB" -s "$ADB_DEVICE" shell pm uninstall "$pkg" >/dev/null 2>&1 || true
            done
            # Trim system app caches to reclaim space (Chrome, GMS, Search fill up over time)
            "$ADB" -s "$ADB_DEVICE" shell pm trim-caches 1024G >/dev/null 2>&1 || true
            "$ADB" -s "$ADB_DEVICE" install "$APK_PATH"
            "$ADB" -s "$ADB_DEVICE" shell monkey -p "${BUNDLE_ID}.debug" -c android.intent.category.LAUNCHER 1 2>&1
            echo "    App launched on Android ($ADB_DEVICE)"
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

        # The .app built by cargo tauri is always in APPLE_BUILD_DIR/arm64-sim/
        IOS_APP="$APPLE_BUILD_DIR/arm64-sim/WDC Shopping.app"
        if [ ! -d "$IOS_APP" ]; then
            # Fallback: search DerivedData (older Tauri versions)
            IOS_APP=$(find ~/Library/Developer/Xcode/DerivedData/wdc-shopping-desktop-*/Build/Products/debug-iphonesimulator -name "*.app" -type d 2>/dev/null | head -1)
        fi
        echo ""
        echo ">>> Build complete!"
        echo "    App: $IOS_APP"

        if [ "$DEPLOY" = true ]; then
            echo ""
            # Auto-detect the first booted iOS simulator matching the requested form factor.
            # "tablet" matches simulators whose name contains "iPad"; everything else is "phone".
            # UUID is extracted via regex (8-4-4-4-12 hex) — robust regardless of chip name in parentheses.
            # Note: grep -qi returns 1 when no match; the || guards it from triggering set -e.
            SIM_UDID=""
            while IFS= read -r line; do
                name=$(echo "$line" | sed 's/ *(.*//' | xargs)
                udid=$(echo "$line" | grep -oE '[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}') || true
                [ -z "$udid" ] && continue
                is_ipad=false
                echo "$name" | grep -qi "ipad" && is_ipad=true || true
                if [ "$FORM_FACTOR" = "tablet" ] && [ "$is_ipad" = true ]; then
                    SIM_UDID="$udid"; break
                elif [ "$FORM_FACTOR" != "tablet" ] && [ "$is_ipad" = false ]; then
                    SIM_UDID="$udid"; break
                fi
            done < <(xcrun simctl list devices booted)

            if [ -z "$SIM_UDID" ]; then
                echo "ERROR: No booted iOS simulator found for form-factor '$FORM_FACTOR'."
                echo "       Boot the appropriate simulator and retry."
                exit 1
            fi

            SIM_NAME=$(xcrun simctl list devices booted | grep "$SIM_UDID" | sed 's/ *(.*//' | xargs)
            echo ">>> Deploying to iOS Simulator: $SIM_NAME ($SIM_UDID, form-factor: $FORM_FACTOR)..."
            xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" 2>/dev/null || true
            xcrun simctl install "$SIM_UDID" "$IOS_APP"
            xcrun simctl launch "$SIM_UDID" "$BUNDLE_ID" > /dev/null
            echo "    App launched on $SIM_NAME"
        fi
        ;;
esac

echo ""
echo ">>> Done!"
