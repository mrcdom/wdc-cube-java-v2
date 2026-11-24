#!/bin/bash
# =============================================================================
# android.sh — local build/run on the Android emulator, from the command line.
#
# `./build.sh android_source` generates a Gradle project, but three local
# environment tweaks are lost on every regeneration (CN1 always recreates the project):
#
#   1. compileSdk/targetSdk — CN1 picks the HIGHEST installed platform. If there is
#      a preview (e.g.: android-37.0), it emits `compileSdkVersion 37`, but
#      Gradle looks for a STABLE `platforms;android-37` (which does not exist) and fails
#      with "Failed to find Platform SDK". Here we pin it to the highest installed
#      stable platform (integer, no dot).
#   2. Gradle JVM — Gradle 8.1 does not run under JDK > 19; we force JDK 17 via
#      org.gradle.java.home (independent of the app's bytecode level, which is 1.8).
#   3. sdk.dir — ensures the SDK location in local.properties.
#
# (Cleartext HTTP and targetSDKVersion already come from build hints in
#  common/codenameone_settings.properties; they need no post-processing.)
#
# Usage:
#   ./android.sh --gen           # regenerates the sources (build.sh android_source), patches, installs and launches
#   ./android.sh                 # only patches the already generated project, installs and launches
#   ./android.sh --gen --patch   # regenerates and patches, but does NOT build — to open/run in an IDE
#   ./android.sh --patch         # only patches the already generated project (no build) — IntelliJ/Studio flow
#
# IDE flow (IntelliJ/Android Studio): run `./android.sh --gen --patch`, then open the generated
# project (android/target/*-android-source) in the IDE and hit Run. `--patch` leaves gradle.properties
# pointing to JDK 17, local.properties with sdk.dir and build.gradle on the stable SDK — which is
# what the IDE honors when syncing Gradle. Do NOT regenerate from inside the IDE (it recreates without the tweaks).
#
# Prerequisites (without --patch): Android emulator ALREADY running; backend up (work/bin/start-server.sh).
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"
PROJ_ROOT="$(pwd)"

# --- Configurable per environment -----------------------------------------------
PKG="${PKG:-br.com.wdc.store.view.remote.shell.cn1}"
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
JDK17="${JDK17:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo /Library/Java/JavaVirtualMachines/microsoft-17.jdk/Contents/Home)}"
ADB="$ANDROID_SDK/platform-tools/adb"

# Highest installed STABLE platform (android-<integer>, without a preview ".x").
# Override manual: SDK=36 ./android.sh
detect_sdk() {
  ls "$ANDROID_SDK/platforms" 2>/dev/null \
    | sed -n 's/^android-\([0-9][0-9]*\)$/\1/p' \
    | sort -n | tail -1
}
SDK_TARGET="${SDK:-$(detect_sdk)}"

# --- Sanity checks --------------------------------------------------------------
[ -d "$ANDROID_SDK" ]    || { echo "ERROR: Android SDK not found at $ANDROID_SDK (set ANDROID_HOME)"; exit 1; }
[ -d "$JDK17" ]          || { echo "ERROR: JDK 17 not found at $JDK17 (set JDK17)"; exit 1; }
[ -n "$SDK_TARGET" ]     || { echo "ERROR: no stable android-<n> platform installed in $ANDROID_SDK/platforms"; exit 1; }

# --- Flags ---------------------------------------------------------------------
DO_GEN=0; PATCH_ONLY=0
for a in "$@"; do
  case "$a" in
    --gen)   DO_GEN=1 ;;
    --patch) PATCH_ONLY=1 ;;
    *) echo "usage: ./android.sh [--gen] [--patch]"; exit 1 ;;
  esac
done

# --- (1) Regenerate the sources, if requested ----------------------------------
if [ "$DO_GEN" == "1" ]; then
  echo "==> Regenerating the Android project (build.sh android_source)…"
  ./build.sh android_source
fi

# --- Locate the generated project (most recent) --------------------------------
GEN="$(ls -dt "$PROJ_ROOT"/android/target/*-android-source 2>/dev/null | head -1)"
[ -n "$GEN" ] && [ -d "$GEN" ] || { echo "ERROR: generated project not found. Run with --gen first."; exit 1; }
echo "==> Project: $GEN"
echo "==> compileSdk/targetSdk -> $SDK_TARGET | Gradle JVM -> $JDK17"

# --- (1) compileSdk/targetSdk to the stable platform ---------------------------
sed -i '' -E "s/compileSdkVersion[[:space:]]+[0-9]+/compileSdkVersion $SDK_TARGET/" "$GEN/app/build.gradle"
sed -i '' -E "s/targetSdkVersion[[:space:]]+[0-9]+/targetSdkVersion $SDK_TARGET/"   "$GEN/app/build.gradle"

# --- (2) Gradle JVM = JDK 17 (overrides the generated org.gradle.java.home) -----
GP="$GEN/gradle.properties"
touch "$GP"
if grep -q '^org\.gradle\.java\.home=' "$GP"; then
  sed -i '' -E "s#^org\.gradle\.java\.home=.*#org.gradle.java.home=$JDK17#" "$GP"
else
  printf '\norg.gradle.java.home=%s\n' "$JDK17" >> "$GP"
fi

# --- (3) sdk.dir in local.properties -------------------------------------------
printf 'sdk.dir=%s\n' "$ANDROID_SDK" > "$GEN/local.properties"

# --- IDE mode: only patches and exits (open/run in the IDE) --------------------
if [ "$PATCH_ONLY" == "1" ]; then
  echo "==> Project patched. Open it in the IDE and hit Run (do NOT regenerate from inside it):"
  echo "    $GEN"
  exit 0
fi

# --- Build + install -----------------------------------------------------------
if ! "$ADB" devices | grep -qE '\bdevice$'; then
  echo "WARNING: no Android emulator/device connected (adb). Start the AVD first."
fi
echo "==> ./gradlew installDebug"
( cd "$GEN" && ANDROID_HOME="$ANDROID_SDK" JAVA_HOME="$JDK17" ./gradlew --no-daemon installDebug )

# --- Launch --------------------------------------------------------------------
echo "==> Launching $PKG on the emulator…"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
echo "==> Done."

