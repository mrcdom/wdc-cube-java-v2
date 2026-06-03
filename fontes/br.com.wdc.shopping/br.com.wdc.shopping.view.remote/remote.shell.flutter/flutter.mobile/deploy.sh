#!/bin/bash
set -e
cd "$(dirname "$0")"

# ──────────────────────────────────────────────────────────────────────────────
# Shopping Remote - Deploy Script
# Builds and/or runs the Flutter mobile app on supported device types.
#
# Usage:
#   ./deploy.sh [command] [target] [options]
#
# Commands:
#   run       Run on a connected device or simulator (default)
#   build     Build release artifact (APK, IPA, etc.)
#   install   Build and install on connected device
#   list      List available devices
#
# Targets:
#   ios           iOS device (physical)
#   ios-sim       iPhone Simulator
#   ipad-sim      iPad Simulator
#   android            Android device (physical)
#   android-emu        Android phone Emulator
#   android-tablet-emu Android tablet Emulator
#   all                Build for all platforms
#
# Options:
#   --release     Build in release mode (default for 'build')
#   --debug       Build in debug mode (default for 'run')
#   --endpoint=X  Backend endpoint (default: http://localhost:8080)
#
# Examples:
# ./deploy.sh list                    # Listar dispositivos disponíveis
# ./deploy.sh run ios-sim             # Executar no simulador iOS
# ./deploy.sh run ios                 # Executar em iPhone físico
# ./deploy.sh run android             # Executar em device Android
# ./deploy.sh run android-emu         # Executar no emulador Android
# ./deploy.sh build all               # Build release para iOS + Android (APK + AAB)
# ./deploy.sh build android           # Só APK
# ./deploy.sh build ios               # Só iOS
# ./deploy.sh install ios-sim         # Build + instalar no iPhone simulator
# ./deploy.sh install ipad-sim        # Build + instalar no iPad simulator
# ./deploy.sh install android         # Build + instalar no device
# ──────────────────────────────────────────────────────────────────────────────

ENDPOINT="${WDC_ENDPOINT:-http://localhost:8080}"
COMMAND="run"
TARGET=""
MODE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    run|build|install|list)
      COMMAND="$1"
      ;;
    ios|ios-sim|ipad-sim|android|android-emu|android-tablet-emu|all)
      TARGET="$1"
      ;;
    --release)
      MODE="--release"
      ;;
    --debug)
      MODE="--debug"
      ;;
    --endpoint=*)
      ENDPOINT="${1#*=}"
      ;;
    -h|--help)
      head -35 "$0" | tail -30
      exit 0
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
  shift
done

DART_DEFINES="--dart-define=WDC_ENDPOINT=$ENDPOINT"

# ──────────────────────────────────────────────────────────────────────────────

list_devices() {
  echo "Available devices:"
  echo ""
  flutter devices
}

find_ios_simulator() {
  # $1 = device filter: "iPhone" or "iPad" (default: "iPhone")
  local filter="${1:-iPhone}"

  # Try to find a booted simulator matching the filter
  local device_id
  device_id=$(xcrun simctl list devices booted -j 2>/dev/null | python3 -c "
import json, sys
filter_name = '$filter'
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    for d in devices:
        if d.get('state') == 'Booted' and filter_name in d.get('name', ''):
            print(d['udid'])
            sys.exit(0)
" 2>/dev/null)

  if [[ -n "$device_id" ]]; then
    echo "$device_id"
  else
    # Boot the first available simulator matching the filter
    device_id=$(xcrun simctl list devices available -j | python3 -c "
import json, sys
filter_name = '$filter'
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    if 'iOS' in runtime:
        for d in devices:
            if filter_name in d.get('name', '') and d.get('isAvailable', False):
                print(d['udid'])
                sys.exit(0)
" 2>/dev/null)
    if [[ -n "$device_id" ]]; then
      echo "Booting simulator $device_id..." >&2
      xcrun simctl boot "$device_id" 2>/dev/null || true
      open -a Simulator
      echo "$device_id"
    else
      echo "ERROR: No $filter simulator found." >&2
      exit 1
    fi
  fi
}

run_on_target() {
  local target="$1"
  local mode="${MODE:---debug}"

  case "$target" in
    ios)
      echo "▶ Running on iOS device -> $ENDPOINT"
      flutter run $mode $DART_DEFINES
      ;;
    ios-sim)
      local sim_id
      sim_id=$(find_ios_simulator iPhone)
      echo "▶ Running on iPhone Simulator ($sim_id) -> $ENDPOINT"
      flutter run -d "$sim_id" $mode $DART_DEFINES
      ;;
    ipad-sim)
      local sim_id
      sim_id=$(find_ios_simulator iPad)
      echo "▶ Running on iPad Simulator ($sim_id) -> $ENDPOINT"
      flutter run -d "$sim_id" $mode $DART_DEFINES
      ;;
    android)
      local android_id
      android_id=$(flutter devices --machine 2>/dev/null | python3 -c "
import json, sys
devices = json.load(sys.stdin)
for d in devices:
    if d.get('targetPlatform','').startswith('android') and not d.get('emulator', False):
        print(d['id']); sys.exit(0)
print(''); sys.exit(1)
" 2>/dev/null)
      if [[ -z "$android_id" ]]; then
        echo "ERROR: No physical Android device found. Use './deploy.sh list' to check."
        exit 1
      fi
      echo "▶ Running on Android device ($android_id) -> $ENDPOINT"
      flutter run -d "$android_id" $mode $DART_DEFINES
      ;;
    android-emu)
      local emu_id
      emu_id=$(flutter devices --machine 2>/dev/null | python3 -c "
import json, sys
devices = json.load(sys.stdin)
tablet_keywords = ['tablet', 'tab', 'nexus 9', 'nexus 10', 'pixel c']
for d in devices:
    if d.get('targetPlatform','').startswith('android') and d.get('emulator', False):
        name = d.get('name','').lower()
        if not any(k in name for k in tablet_keywords):
            print(d['id']); sys.exit(0)
# fallback: any android emulator
for d in devices:
    if d.get('targetPlatform','').startswith('android') and d.get('emulator', False):
        print(d['id']); sys.exit(0)
print(''); sys.exit(1)
" 2>/dev/null)
      if [[ -z "$emu_id" ]]; then
        echo "ERROR: No Android phone emulator found. Use './deploy.sh list' to check."
        exit 1
      fi
      # Android emulator uses 10.0.2.2 to reach host's localhost
      local emu_endpoint="${ENDPOINT//localhost/10.0.2.2}"
      emu_endpoint="${emu_endpoint//127.0.0.1/10.0.2.2}"
      local emu_dart_defines="--dart-define=WDC_ENDPOINT=$emu_endpoint"
      echo "▶ Running on Android Phone Emulator ($emu_id) -> $emu_endpoint"
      flutter run -d "$emu_id" $mode $emu_dart_defines
      ;;
    android-tablet-emu)
      local tablet_id
      tablet_id=$(flutter devices --machine 2>/dev/null | python3 -c "
import json, sys
devices = json.load(sys.stdin)
tablet_keywords = ['tablet', 'tab', 'nexus 9', 'nexus 10', 'pixel c']
for d in devices:
    if d.get('targetPlatform','').startswith('android') and d.get('emulator', False):
        name = d.get('name','').lower()
        if any(k in name for k in tablet_keywords):
            print(d['id']); sys.exit(0)
print(''); sys.exit(1)
" 2>/dev/null)
      if [[ -z "$tablet_id" ]]; then
        echo "ERROR: No Android tablet emulator found."
        echo "  Make sure the AVD name contains 'Tablet' or 'Tab'."
        echo "  Use './deploy.sh list' to check available devices."
        exit 1
      fi
      # Android emulator uses 10.0.2.2 to reach host's localhost
      local emu_endpoint="${ENDPOINT//localhost/10.0.2.2}"
      emu_endpoint="${emu_endpoint//127.0.0.1/10.0.2.2}"
      local emu_dart_defines="--dart-define=WDC_ENDPOINT=$emu_endpoint"
      echo "▶ Running on Android Tablet Emulator ($tablet_id) -> $emu_endpoint"
      flutter run -d "$tablet_id" $mode $emu_dart_defines
      ;;
    *)
      echo "ERROR: Specify a target: ios, ios-sim, android, android-emu"
      exit 1
      ;;
  esac
}

build_target() {
  local target="$1"
  local mode="${MODE:---release}"

  case "$target" in
    ios|ios-sim|ipad-sim)
      echo "🔨 Building iOS ($mode) -> $ENDPOINT"
      flutter build ios $mode $DART_DEFINES --no-codesign
      echo ""
      echo "✓ iOS build at: build/ios/iphoneos/Runner.app"
      ;;
    android|android-emu)
      echo "🔨 Building Android APK ($mode) -> $ENDPOINT"
      flutter build apk $mode $DART_DEFINES
      echo ""
      echo "✓ APK at: build/app/outputs/flutter-apk/app-release.apk"
      ;;
    all)
      echo "🔨 Building ALL platforms ($mode) -> $ENDPOINT"
      echo ""
      echo "── iOS ──"
      flutter build ios $mode $DART_DEFINES --no-codesign
      echo "✓ iOS build at: build/ios/iphoneos/Runner.app"
      echo ""
      echo "── Android APK ──"
      flutter build apk $mode $DART_DEFINES
      echo "✓ APK at: build/app/outputs/flutter-apk/app-release.apk"
      echo ""
      echo "── Android App Bundle ──"
      flutter build appbundle $mode $DART_DEFINES
      echo "✓ AAB at: build/app/outputs/bundle/release/app-release.aab"
      echo ""
      echo "All builds complete."
      ;;
    *)
      echo "ERROR: Specify a target: ios, android, all"
      exit 1
      ;;
  esac
}

install_on_target() {
  local target="$1"
  local mode="${MODE:---release}"

  case "$target" in
    ios)
      echo "📲 Building and installing on iOS device..."
      flutter build ios $mode $DART_DEFINES --no-codesign
      flutter install -d ios
      ;;
    ios-sim)
      local sim_id
      sim_id=$(find_ios_simulator iPhone)
      echo "📲 Building and installing on iPhone Simulator ($sim_id)..."
      flutter build ios --debug $DART_DEFINES --simulator
      xcrun simctl install "$sim_id" build/ios/iphonesimulator/Runner.app
      echo "✓ Installed on simulator. Launch from Home screen."
      ;;
    ipad-sim)
      local sim_id
      sim_id=$(find_ios_simulator iPad)
      echo "📲 Building and installing on iPad Simulator ($sim_id)..."
      flutter build ios --debug $DART_DEFINES --simulator
      xcrun simctl install "$sim_id" build/ios/iphonesimulator/Runner.app
      echo "✓ Installed on simulator. Launch from Home screen."
      ;;
    android|android-emu)
      echo "📲 Building and installing on Android..."
      flutter build apk $mode $DART_DEFINES
      flutter install -d android
      ;;
    *)
      echo "ERROR: Specify a target: ios, ios-sim, android, android-emu"
      exit 1
      ;;
  esac
}

# ──────────────────────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────────────────────

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Shopping Remote - Deploy"
echo "  Command: $COMMAND | Target: ${TARGET:-auto} | Endpoint: $ENDPOINT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

case "$COMMAND" in
  list)
    list_devices
    ;;
  run)
    if [[ -z "$TARGET" ]]; then
      echo "ERROR: Specify a target for 'run': ios, ios-sim, ipad-sim, android, android-emu, android-tablet-emu"
      echo "  Use './deploy.sh list' to see available devices."
      exit 1
    fi
    run_on_target "$TARGET"
    ;;
  build)
    if [[ -z "$TARGET" ]]; then
      TARGET="all"
    fi
    build_target "$TARGET"
    ;;
  install)
    if [[ -z "$TARGET" ]]; then
      echo "ERROR: Specify a target for 'install': ios, ios-sim, ipad-sim, android, android-emu, android-tablet-emu"
      exit 1
    fi
    install_on_target "$TARGET"
    ;;
esac
