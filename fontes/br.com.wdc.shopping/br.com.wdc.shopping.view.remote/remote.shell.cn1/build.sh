#!/bin/bash
set -e
MVNW="./mvnw"

function mac_desktop {

  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javase" "-Dcodename1.buildTarget=mac-os-x-desktop" "-U" "-e"
}
function mac_native {

  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=mac-os-x-native" "-U" "-e"
}
function windows_desktop {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javase" "-Dcodename1.buildTarget=windows-desktop" "-U" "-e"
}
function windows_device {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=win" "-Dcodename1.buildTarget=windows-device" "-U" "-e"
}
function uwp {

  "windows_device"
}
function linux_device {

  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=linux" "-Dcodename1.buildTarget=linux-device" "-U" "-e"
}
function javascript {

  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javascript" "-Dcodename1.buildTarget=javascript" "-U" "-e"
  deploy_javascript
}

# Deploys the web app (cloud build result) + context.html to work/frontend/remote.shell.cn1,
# just like remote.shell.teavm/react. The backend landing page lists the folder and links /<ctx>/index.html.
function deploy_javascript {
  local DEPLOY_DIR="../../../../work/frontend/remote.shell.cn1"
  # the CN1 cloud build returns the web app — looks for, in order: result.zip, a web .war,
  # or an already extracted index.html (ignores the -jar-with-dependencies input jar).
  local ART IDX
  ART=$(find javascript/target \( -name "result.zip" -o -name "*.war" \) 2>/dev/null | head -1)
  IDX=$(find javascript/target -name "index.html" 2>/dev/null | head -1)
  if [ -n "$ART" ]; then
    echo "Deploying JS from: $ART"
    rm -rf "$DEPLOY_DIR"; mkdir -p "$DEPLOY_DIR"
    local TMP; TMP=$(mktemp -d)
    unzip -oq "$ART" -d "$TMP"
    local WAR; WAR=$(find "$TMP" -name "*.war" 2>/dev/null | head -1)
    if [ -n "$WAR" ]; then
      unzip -oq "$WAR" -d "$DEPLOY_DIR" -x "WEB-INF/*" "META-INF/*" # the web app lives at the .war root
    else
      cp -R "$TMP"/. "$DEPLOY_DIR"/ # the zip itself is the web app
    fi
    rm -rf "$TMP"
  elif [ -n "$IDX" ]; then
    echo "Deploying JS from: $(dirname "$IDX")"
    rm -rf "$DEPLOY_DIR"; mkdir -p "$DEPLOY_DIR"
    cp -R "$(dirname "$IDX")"/. "$DEPLOY_DIR"/
  else
    echo "WARNING: no web app (result.zip/.war/index.html) in javascript/target — only the input jar."
    echo "         The CN1 cloud build did not return the web artifact. Check that:"
    echo "           1) the browser login was completed (token saved in ~/.codenameone);"
    echo "           2) the account has an Enterprise plan (the JavaScript build is an Enterprise feature)."
  fi
  mkdir -p "$DEPLOY_DIR"
  cp "context.html" "$DEPLOY_DIR/context.html"
  echo "context.html copied to: $DEPLOY_DIR"

}
function android {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-device" "-U" "-e"
}
function xcode {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" "-U" "-e"
}
function ios_source {
  "xcode" 
}
function android_source {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" "-U" "-e"
}
function ios {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-device" "-U" "-e"
}
function ios_release {
  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-device-release" "-U" "-e"
}
function jar {
  
  "$MVNW" "-Pexecutable-jar" "package" "-Dcodename1.platform=javase" "-DskipTests" "-U" "-e"
}
function help {
  "echo" "-e" "build.sh [COMMAND]"
  "echo" "-e" "Local Build Commands:"
  "echo" "-e" "  The following commands will build the app locally (i.e. does NOT use the Codename One build server)"
  "echo" "-e" ""
  "echo" "-e" "  jar"
  "echo" "-e" "    Builds app as desktop app executable jar file to javase/target directory"
  "echo" "-e" "  android_source"
  "echo" "-e" "    Generates an android gradle project that can be opened in Android studio"
  "echo" "-e" "    *Requires android development tools installed."
  "echo" "-e" "    *Requires ANDROID_HOME environment variable"
  "echo" "-e" "    *Requires either GRADLE_HOME environment variable, or for gradle to be in PATH"
  "echo" "-e" "  ios_source"
  "echo" "-e" "    Generates an Xcode Project that you can open and build using Apple's development tools"
  "echo" "-e" "    *Requires a Mac with Xcode installed"
  "echo" "-e" ""
  "echo" "-e" "Build Server Commands:"
  "echo" "-e" "  The following commands will build the app using the Codename One build server, and require"
  "echo" "-e" "  a Codename One account.  See https://www.codenameone.com"
  "echo" "-e" ""
  "echo" "-e" "  ios"
  "echo" "-e" "    Builds iOS app."
  "echo" "-e" "  ios_release"
  "echo" "-e" "    Builds iOS app for submission to Apple appstore."
  "echo" "-e" "  android"
  "echo" "-e" "    Builds android app."
  "echo" "-e" "  mac_desktop"
  "echo" "-e" "    Builds Mac OS desktop app."
  "echo" "-e" "    *Mac OS Desktop builds are a Pro user feature."
  "echo" "-e" "  mac_native"
  "echo" "-e" "    Builds a native Mac app (no JVM)."
  "echo" "-e" "  windows_desktop"
  "echo" "-e" "    Builds Windows desktop app."
  "echo" "-e" "    *Windows Desktop builds are a Pro user feature."
  "echo" "-e" "  windows_device"
  "echo" "-e" "    Builds UWP Windows app."
  "echo" "-e" "  linux_device"
  "echo" "-e" "    Builds a native Linux app (ELF, no JVM)."
  "echo" "-e" "  javascript"
  "echo" "-e" "    Builds as a web app."
  "echo" "-e" "    *Javascript builds are an Enterprise user feature"
}
function settings {
  
  "$MVNW" "cn:settings" "-U" "-e"
}
CMD="$1"

if [ "$CMD" == "" ]; then
  CMD="jar"
fi
"$CMD" 