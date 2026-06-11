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
#   ./build.sh --native --sim --deploy              # build + install + launch on simulator (phone)
#   ./build.sh --native --sim --tablet --deploy       # build + install + launch on simulator (tablet)
#   ./build.sh --native --deploy                      # build + install on device
#   ./build.sh --deploy-only                         # install last .app on simulator
#
# Options:
#   --native            Run GluonFX native compile + package (requires GRAALVM_HOME)
#   --sim               Target iOS Simulator instead of physical device
#   --deploy            Deploy after build
#   --deploy-only       Skip build, just install + launch on simulator
#   --phone             Target phone simulator (default; sim name: "iPhone 16 Pro")
#   --tablet            Target tablet simulator (sim name: "iPad Pro 13-inch (M4)")
#   --sim-name=X        Override simulator name explicitly (takes precedence over --phone/--tablet)
#
# KNOWN ISSUE — GluonFX ios-sim on Apple Silicon:
#   GluonFX Substrate 0.0.68 hardcodes the ios-sim native-image target as x86_64,
#   and Gluon never published vmone-ios-macos-x64.zip. The -Pios-sim profile cannot
#   work on Apple Silicon.
#   Workaround: build with the regular ios (arm64) target. Apple Silicon simulators
#   run arm64 device binaries natively, so xcrun simctl install/launch works fine.
#   This script auto-applies the workaround when --sim is used on arm64 hosts.

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
FORM_FACTOR="phone"   # phone | tablet
SIM_NAME_OVERRIDE=""

for arg in "$@"; do
    case "$arg" in
        --deploy)      DEPLOY=true ;;
        --deploy-only) DEPLOY_ONLY=true ;;
        --native)      NATIVE=true ;;
        --sim)         SIM=true ;;
        --phone)       FORM_FACTOR="phone" ;;
        --tablet)      FORM_FACTOR="tablet" ;;
        --sim-name=*)  SIM_NAME_OVERRIDE="${arg#--sim-name=}" ;;
        *) echo "WARNING: Unknown argument: $arg" ;;
    esac
done

# Resolve simulator name: explicit override wins, then detect booted, then form-factor default
if [ -n "$SIM_NAME_OVERRIDE" ]; then
    SIM_NAME="$SIM_NAME_OVERRIDE"
else
    if [ "$FORM_FACTOR" = "tablet" ]; then
        FORM_GREP="iPad"
        DEFAULT_SIM="iPad Pro 13-inch (M4)"
    else
        FORM_GREP="iPhone"
        DEFAULT_SIM="iPhone 16 Pro"
    fi
    # Extract name of the first booted simulator matching the form factor.
    # Strip the UUID suffix " (XXXXXXXX-...) (Booted)" to recover the device name,
    # which may itself contain parentheses (e.g. "iPad Pro 13-inch (M4)").
    BOOTED_SIM=$(xcrun simctl list devices 2>/dev/null \
        | grep "(Booted)" | grep -i "$FORM_GREP" | head -1 \
        | sed 's/ ([A-Z0-9-]\{36\}).*//' | xargs)
    if [ -n "$BOOTED_SIM" ]; then
        SIM_NAME="$BOOTED_SIM"
    else
        SIM_NAME="$DEFAULT_SIM"
    fi
fi

# Set to true when Apple Silicon forces arm64 ios target instead of ios-sim x86_64
SIM_ARM64_WORKAROUND=false
if [ "$SIM" = true ] && [ "$(uname -m)" = "arm64" ]; then
    SIM_ARM64_WORKAROUND=true
fi

# For native builds, switch JAVA_HOME to GraalVM
if [ "$NATIVE" = true ]; then
    if [ -z "$GRAALVM_HOME" ]; then
        echo "ERROR: Set GRAALVM_HOME for native compilation."
        echo "       Found: /Library/Java/JavaVirtualMachines/graalvm-gluon-23.jdk/Contents/Home"
        exit 1
    fi
    export JAVA_HOME="$GRAALVM_HOME"
    export PATH="$GRAALVM_HOME/bin:$PATH"

    # Detect Apple Silicon + ios-sim: the -Pios-sim profile (x86_64) is broken in
    # GluonFX 1.0.25 / Substrate 0.0.68 — Gluon never published vmone-ios-macos-x64.zip.
    # Workaround: compile for ios (arm64 device target). Apple Silicon simulators
    # run arm64 device binaries natively, so xcrun simctl install/launch works fine.
    if [ "$SIM_ARM64_WORKAROUND" = true ]; then
        echo ">>> Apple Silicon: ios-sim profile unavailable — building arm64 (ios target) for simulator"
    fi
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
[ "$SIM" = true ] && echo "  Form factor:  $FORM_FACTOR"
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
        if [ "$SIM" = true ] && [ "$SIM_ARM64_WORKAROUND" = false ]; then
            PROFILE_ARG="-Pios-sim"
        fi
        # SIM_ARM64_WORKAROUND=true: no profile — compiles as ios (arm64), installed to sim via xcrun

        cd "$SCRIPT_DIR"
        if [ "$SIM_ARM64_WORKAROUND" = true ]; then
            # Skip gluonfx:package — it requires a signing certificate even for simulator builds.
            # We assemble the Info.plist manually after the link step.
            echo ">>> [2/2] Native compile + link (GluonFX, skipping package for simulator)..."
            mvn -DskipTests gluonfx:compile gluonfx:link

            # Patch Info.plist: substitute unresolved template variables
            APP_DIR="$SCRIPT_DIR/target/gluonfx/arm64-ios/gluon.ios.app"
            sed \
                -e 's|\$packageName\.\$mainClassName|br.com.wdc.shopping.view.gluon.ios|g' \
                -e 's|\$packageName|br.com.wdc.shopping.view.gluon|g' \
                -e 's|\$mainClassName|ios|g' \
                -e 's|\$mainClassNameApp|gluon.ios|g' \
                -e 's|\$bundleName|Shopping|g' \
                -e 's|\$bundleVersion|1.0|g' \
                -e 's|\$bundleShortVersion|1.0|g' \
                "$SCRIPT_DIR/src/ios/Default-Info.plist" > "$APP_DIR/Info.plist"

            # Patch binary: change platform tag ios → iossimulator so the Simulator accepts it.
            # GluonFX links with iPhoneOS SDK producing platform=IOS; Simulator requires IOSSIMULATOR (=7).
            BINARY="$APP_DIR/gluon.ios"
            vtool -set-build-version 7 11.0 26.5 -replace -output "${BINARY}.patched" "$BINARY"
            mv "${BINARY}.patched" "$BINARY"

            # Recompile Assets.car for iphonesimulator platform.
            # gluonfx:link produces an Assets.car tagged platform=ios (device); the Simulator
            # ignores icons from a device-tagged car, resulting in a blank icon on the launcher.
            # Copy the source PNGs directly to the bundle root — same layout used by gluonfx:package
            # for ios-sim builds (and matches the working Jun 9 app bundle structure).
            ICONS_SRC="$SCRIPT_DIR/src/ios/assets/Assets.xcassets/AppIcon.appiconset"
            cp "$ICONS_SRC/icon-120.png" "$APP_DIR/AppIcon60x60@2x.png"
            cp "$ICONS_SRC/icon-180.png" "$APP_DIR/AppIcon60x60@3x.png"
            cp "$ICONS_SRC/icon-152.png" "$APP_DIR/AppIcon76x76@2x~ipad.png"
            cp "$ICONS_SRC/icon-167.png" "$APP_DIR/AppIcon83.5x83.5@2x~ipad.png"
            # Register icon files in Info.plist (required; CFBundleIconName alone uses Assets.car
            # which is platform=ios and ignored by the Simulator)
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons dict"                                                                        "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons:CFBundlePrimaryIcon dict"                                                    "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons:CFBundlePrimaryIcon:CFBundleIconFiles array"                                 "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons:CFBundlePrimaryIcon:CFBundleIconFiles:0 string AppIcon60x60"                 "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons~ipad dict"                                                                   "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons~ipad:CFBundlePrimaryIcon dict"                                               "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons~ipad:CFBundlePrimaryIcon:CFBundleIconFiles array"                            "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons~ipad:CFBundlePrimaryIcon:CFBundleIconFiles:0 string AppIcon76x76"            "$APP_DIR/Info.plist"
            /usr/libexec/PlistBuddy -c "Add :CFBundleIcons~ipad:CFBundlePrimaryIcon:CFBundleIconFiles:1 string AppIcon83.5x83.5"        "$APP_DIR/Info.plist"

            # Ad-hoc sign the bundle (required after binary modification; Simulator accepts -s -)
            codesign -f -s - "$APP_DIR"
        else
            echo ">>> [2/2] Native compile + package (GluonFX)..."
            mvn -DskipTests gluonfx:compile gluonfx:link gluonfx:package $PROFILE_ARG
        fi
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
    # Find the .app — prefer arm64-ios when Apple Silicon workaround is active
    if [ "$SIM_ARM64_WORKAROUND" = true ]; then
        IOS_APP=$(find "$SCRIPT_DIR/target/gluonfx/arm64-ios" -name "*.app" -type d 2>/dev/null | head -1)
    else
        IOS_APP=$(find "$SCRIPT_DIR/target/gluonfx" -name "*.app" -type d 2>/dev/null | head -1)
    fi

    if [ -z "$IOS_APP" ]; then
        echo "ERROR: No .app found in target/gluonfx/"
        echo "       Run with --native first to build the native app."
        exit 1
    fi

    echo ">>> App: $IOS_APP"

    if [ "$SIM" = true ] || [ "$DEPLOY_ONLY" = true ]; then
        # Determine the bundle ID from the installed .app Info.plist
        IOS_BUNDLE_ID=$(/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$IOS_APP/Info.plist" 2>/dev/null || echo "$BUNDLE_ID")
        echo ""
        echo ">>> Deploying to iOS Simulator ($SIM_NAME)..."
        [ "$SIM_ARM64_WORKAROUND" = true ] && echo "    (arm64 device build installed on Apple Silicon simulator)"
        echo "    Bundle ID: $IOS_BUNDLE_ID"
        xcrun simctl boot "$SIM_NAME" 2>/dev/null || true
        xcrun simctl terminate "$SIM_NAME" "$IOS_BUNDLE_ID" 2>/dev/null || true
        xcrun simctl install "$SIM_NAME" "$IOS_APP"
        xcrun simctl launch "$SIM_NAME" "$IOS_BUNDLE_ID"
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
