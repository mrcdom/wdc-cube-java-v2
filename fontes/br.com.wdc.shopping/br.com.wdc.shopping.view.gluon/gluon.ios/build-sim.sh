#!/usr/bin/env bash
set -euo pipefail

#
# Build, deploy and run the GluonFX iOS app on the iPhone simulator.
#
# Usage:
#   ./build-sim.sh              # full build (compile + link + deploy + launch)
#   ./build-sim.sh --deploy     # skip compile/link, just deploy + launch (reuse last build)
#   ./build-sim.sh --no-launch  # build + deploy, but don't launch
#

# ── Configuration ──────────────────────────────────────────────────────────────

GRAALVM_HOME="${GRAALVM_HOME:-/Library/Java/JavaVirtualMachines/graalvm-gluon-23.jdk/Contents/Home}"
JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home}"
export GRAALVM_HOME JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SHOPPING_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ARTIFACT_ID="br.com.wdc.shopping.view.gluon.ios"
APP_NAME="gluon.ios"
APP_BUNDLE="$SCRIPT_DIR/target/gluonfx/arm64-ios/${APP_NAME}.app"
BINARY="$APP_BUNDLE/$APP_NAME"

# Simulator – auto-detect first booted device, or override with SIM_UDID env var
if [[ -z "${SIM_UDID:-}" ]]; then
    SIM_UDID="$(xcrun simctl list devices booted -j \
        | python3 -c "import sys,json; devs=[d for r in json.loads(sys.stdin.read())['devices'].values() for d in r if d['state']=='Booted']; print(devs[0]['udid'] if devs else '')" 2>/dev/null || true)"
    if [[ -z "$SIM_UDID" ]]; then
        echo "❌ No booted simulator found. Boot one first:" >&2
        echo "   xcrun simctl boot <UDID>" >&2
        exit 1
    fi
fi

MIN_IOS="14.0"
SDK_VERSION="18.2"

# ── Parse arguments ────────────────────────────────────────────────────────────

SKIP_BUILD=false
NO_LAUNCH=false
for arg in "$@"; do
    case "$arg" in
        --deploy)    SKIP_BUILD=true ;;
        --no-launch) NO_LAUNCH=true ;;
        -h|--help)
            echo "Usage: $0 [--deploy] [--no-launch]"
            echo "  --deploy     Skip native compile/link, just deploy + launch"
            echo "  --no-launch  Build + deploy, but don't launch the app"
            exit 0
            ;;
        *) echo "Unknown option: $arg"; exit 1 ;;
    esac
done

# ── Step 1: Install dependencies ──────────────────────────────────────────────

if [[ "$SKIP_BUILD" == false ]]; then
    echo "▶ Installing presentation module..."
    (cd "$SHOPPING_ROOT" && mvn install -pl br.com.wdc.shopping.presentation -q)
    echo "  ✔ presentation installed"

    # ── Step 2: Native compile + link ──────────────────────────────────────────
    echo "▶ Native compile + link..."
    cd "$SCRIPT_DIR"
    rm -rf target
    mvn gluonfx:compile gluonfx:link
    echo "  ✔ native image built"
fi

# ── Step 3: Write Info.plist ──────────────────────────────────────────────────

echo "▶ Writing Info.plist..."
cat > "$APP_BUNDLE/Info.plist" << PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIdentifier</key>
    <string>${ARTIFACT_ID}</string>
    <key>CFBundleName</key>
    <string>Shopping</string>
    <key>CFBundleVersion</key>
    <string>1.0</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleIcons</key>
    <dict>
        <key>CFBundlePrimaryIcon</key>
        <dict>
            <key>CFBundleIconFiles</key>
            <array>
                <string>AppIcon60x60</string>
            </array>
            <key>CFBundleIconName</key>
            <string>AppIcon</string>
        </dict>
    </dict>
    <key>CFBundleIcons~ipad</key>
    <dict>
        <key>CFBundlePrimaryIcon</key>
        <dict>
            <key>CFBundleIconFiles</key>
            <array>
                <string>AppIcon60x60</string>
                <string>AppIcon76x76</string>
            </array>
            <key>CFBundleIconName</key>
            <string>AppIcon</string>
        </dict>
    </dict>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>MinimumOSVersion</key>
    <string>${MIN_IOS}</string>
    <key>UIDeviceFamily</key>
    <array>
        <integer>1</integer>
        <integer>2</integer>
    </array>
    <key>UIRequiresFullScreen</key>
    <true/>
    <key>UIStatusBarHidden</key>
    <false/>
    <key>UIViewControllerBasedStatusBarAppearance</key>
    <false/>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
    <key>UISupportedInterfaceOrientations~ipad</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationPortraitUpsideDown</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsArbitraryLoads</key>
        <true/>
    </dict>
    <key>UILaunchScreen</key>
    <dict/>
</dict>
</plist>
PLIST
echo "  ✔ Info.plist written"

# ── Step 3b: Copy application config into bundle ──────────────────────────────

CONFIG_SRC="$SCRIPT_DIR/src/main/resources/config/application.toml"
if [[ -f "$CONFIG_SRC" ]]; then
    mkdir -p "$APP_BUNDLE/config"
    cp "$CONFIG_SRC" "$APP_BUNDLE/config/application.toml"
    echo "  ✔ application.toml copied to bundle"
fi

# ── Step 4: Compile asset catalog (icons) ─────────────────────────────────────

ASSETS_DIR="$SCRIPT_DIR/src/ios/assets/Assets.xcassets"
if [[ -d "$ASSETS_DIR" ]]; then
    echo "▶ Compiling asset catalog..."
    ICON_SET="$ASSETS_DIR/AppIcon.appiconset"
    # Try actool with clean env; fall back to direct PNG copy
    ACTOOL_OUT=$(mktemp -d)
    if env -i PATH="/usr/bin:/bin:/usr/sbin:/sbin" HOME="$HOME" \
        DEVELOPER_DIR="/Applications/Xcode.app/Contents/Developer" \
        xcrun actool "$ASSETS_DIR" \
            --compile "$ACTOOL_OUT" \
            --platform iphonesimulator \
            --minimum-deployment-target "$MIN_IOS" \
            --app-icon AppIcon \
            --target-device iphone \
            --output-partial-info-plist /tmp/assetcatalog_partial.plist 2>/dev/null \
        && [[ -f "$ACTOOL_OUT/Assets.car" ]]; then
        cp "$ACTOOL_OUT/Assets.car" "$APP_BUNDLE/"
        echo "  ✔ asset catalog compiled (Assets.car)"
    else
        # actool may generate named PNGs even if .car fails
        if ls "$ACTOOL_OUT"/AppIcon*.png >/dev/null 2>&1; then
            cp "$ACTOOL_OUT"/AppIcon*.png "$APP_BUNDLE/"
        fi
        # Ensure all required icon PNGs are present
        if [[ -d "$ICON_SET" ]]; then
            cp "$ICON_SET/icon-120.png" "$APP_BUNDLE/AppIcon60x60@2x.png" 2>/dev/null || true
            cp "$ICON_SET/icon-180.png" "$APP_BUNDLE/AppIcon60x60@3x.png" 2>/dev/null || true
            cp "$ICON_SET/icon-152.png" "$APP_BUNDLE/AppIcon76x76@2x~ipad.png" 2>/dev/null || true
        fi
        echo "  ✔ icon PNGs copied to bundle"
    fi
    rm -rf "$ACTOOL_OUT"
else
    echo "  ⚠ No Assets.xcassets found, skipping icon"
fi

# ── Step 5: Re-stamp binary for simulator ─────────────────────────────────────

echo "▶ Re-stamping binary (platform 7 = iOSSimulator)..."
vtool -set-build-version 7 "$MIN_IOS" "$SDK_VERSION" -replace -output "$BINARY" "$BINARY"
echo "  ✔ vtool done"

# ── Step 6: Codesign ──────────────────────────────────────────────────────────

echo "▶ Codesigning..."
codesign --force --sign - "$APP_BUNDLE"
echo "  ✔ codesigned"

# ── Step 7: Install on simulator ──────────────────────────────────────────────

echo "▶ Installing on simulator ($SIM_UDID)..."
xcrun simctl install "$SIM_UDID" "$APP_BUNDLE"
echo "  ✔ installed"

# ── Step 8: Launch ────────────────────────────────────────────────────────────

if [[ "$NO_LAUNCH" == true ]]; then
    echo "✅ Build + deploy complete (launch skipped)."
    exit 0
fi

# Resolve the installed bundle path so we can pass the config file location
INSTALLED_BUNDLE="$(xcrun simctl get_app_container "$SIM_UDID" "$ARTIFACT_ID" app 2>/dev/null || true)"

echo "▶ Launching..."
xcrun simctl launch --console "$SIM_UDID" "$ARTIFACT_ID" \
    -Dshopping.config.file="${INSTALLED_BUNDLE}/config/application.toml" 2>&1
