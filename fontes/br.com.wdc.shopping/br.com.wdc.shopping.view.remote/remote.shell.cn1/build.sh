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

# Deposita o app web (resultado do cloud build) + context.html em work/frontend/remote.shell.cn1,
# tal qual remote.shell.teavm/react. A landing do backend lista a pasta e linka /<ctx>/index.html.
function deploy_javascript {
  local DEPLOY_DIR="../../../../work/frontend/remote.shell.cn1"
  # o cloud build do CN1 devolve o app web — procura, em ordem: result.zip, um .war de web,
  # ou um index.html já extraído (ignora o jar de input -jar-with-dependencies).
  local ART IDX
  ART=$(find javascript/target \( -name "result.zip" -o -name "*.war" \) 2>/dev/null | head -1)
  IDX=$(find javascript/target -name "index.html" 2>/dev/null | head -1)
  if [ -n "$ART" ]; then
    echo "Deploy do JS a partir de: $ART"
    rm -rf "$DEPLOY_DIR"; mkdir -p "$DEPLOY_DIR"
    local TMP; TMP=$(mktemp -d)
    unzip -oq "$ART" -d "$TMP"
    local WAR; WAR=$(find "$TMP" -name "*.war" 2>/dev/null | head -1)
    if [ -n "$WAR" ]; then
      unzip -oq "$WAR" -d "$DEPLOY_DIR" -x "WEB-INF/*" "META-INF/*" # web vive na raiz do .war
    else
      cp -R "$TMP"/. "$DEPLOY_DIR"/ # o próprio zip já é o app web
    fi
    rm -rf "$TMP"
  elif [ -n "$IDX" ]; then
    echo "Deploy do JS a partir de: $(dirname "$IDX")"
    rm -rf "$DEPLOY_DIR"; mkdir -p "$DEPLOY_DIR"
    cp -R "$(dirname "$IDX")"/. "$DEPLOY_DIR"/
  else
    echo "AVISO: nenhum app web (result.zip/.war/index.html) em javascript/target — só o jar de input."
    echo "       O cloud build do CN1 não retornou o artefato web. Confira:"
    echo "         1) o login no navegador foi concluído (token salvo em ~/.codenameone);"
    echo "         2) a conta tem plano Enterprise (build JavaScript é recurso Enterprise)."
  fi
  mkdir -p "$DEPLOY_DIR"
  cp "context.html" "$DEPLOY_DIR/context.html"
  echo "context.html copiado em: $DEPLOY_DIR"
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