#!/bin/bash
# =============================================================================
# android.sh — build/run local no emulador Android, via linha de comando.
#
# O `./build.sh android_source` gera um projeto Gradle, mas três ajustes do
# ambiente local se perdem a cada regeração (o CN1 sempre recria o projeto):
#
#   1. compileSdk/targetSdk — o CN1 escolhe a MAIOR plataforma instalada. Se há
#      um preview (ex.: android-37.0), ele emite `compileSdkVersion 37`, mas o
#      Gradle procura `platforms;android-37` ESTÁVEL (que não existe) e falha
#      com "Failed to find Platform SDK". Aqui fixamos na maior plataforma
#      estável (inteira, sem ponto) instalada.
#   2. Gradle JVM — o Gradle 8.1 não roda sob JDK > 19; forçamos JDK 17 via
#      org.gradle.java.home (independe do nível de bytecode do app, que é 1.8).
#   3. sdk.dir — garante o apontamento do SDK em local.properties.
#
# (O cleartext HTTP e o targetSDKVersion já vêm de build hints no
#  common/codenameone_settings.properties; não precisam de pós-processamento.)
#
# Uso:
#   ./android.sh --gen           # regenera os fontes (build.sh android_source), corrige, instala e abre
#   ./android.sh                 # só corrige o projeto já gerado, instala e abre
#   ./android.sh --gen --patch   # regenera e corrige, mas NÃO builda — para abrir/rodar numa IDE
#   ./android.sh --patch         # só corrige o projeto já gerado (sem build) — fluxo IntelliJ/Studio
#
# Fluxo IDE (IntelliJ/Android Studio): rode `./android.sh --gen --patch`, depois abra o projeto
# gerado (android/target/*-android-source) na IDE e dê Run. O `--patch` deixa gradle.properties
# apontando o JDK 17, o local.properties com o sdk.dir e o build.gradle no SDK estável — que é o
# que a IDE respeita ao sincronizar o Gradle. NÃO regere de dentro da IDE (recria sem os ajustes).
#
# Pré-requisitos (sem --patch): emulador Android JÁ rodando; backend no ar (work/bin/start-server.sh).
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"
PROJ_ROOT="$(pwd)"

# --- Configuráveis por ambiente -------------------------------------------------
PKG="${PKG:-br.com.wdc.shopping.view.remote.shell.cn1}"
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
JDK17="${JDK17:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo /Library/Java/JavaVirtualMachines/microsoft-17.jdk/Contents/Home)}"
ADB="$ANDROID_SDK/platform-tools/adb"

# Maior plataforma ESTÁVEL instalada (android-<inteiro>, sem ".x" de preview).
# Override manual: SDK=36 ./android.sh
detect_sdk() {
  ls "$ANDROID_SDK/platforms" 2>/dev/null \
    | sed -n 's/^android-\([0-9][0-9]*\)$/\1/p' \
    | sort -n | tail -1
}
SDK_TARGET="${SDK:-$(detect_sdk)}"

# --- Sanidade -------------------------------------------------------------------
[ -d "$ANDROID_SDK" ]    || { echo "ERRO: Android SDK não encontrado em $ANDROID_SDK (defina ANDROID_HOME)"; exit 1; }
[ -d "$JDK17" ]          || { echo "ERRO: JDK 17 não encontrado em $JDK17 (defina JDK17)"; exit 1; }
[ -n "$SDK_TARGET" ]     || { echo "ERRO: nenhuma plataforma android-<n> estável instalada em $ANDROID_SDK/platforms"; exit 1; }

# --- Flags ---------------------------------------------------------------------
DO_GEN=0; PATCH_ONLY=0
for a in "$@"; do
  case "$a" in
    --gen)   DO_GEN=1 ;;
    --patch) PATCH_ONLY=1 ;;
    *) echo "uso: ./android.sh [--gen] [--patch]"; exit 1 ;;
  esac
done

# --- (1) Regerar os fontes, se pedido ------------------------------------------
if [ "$DO_GEN" == "1" ]; then
  echo "==> Regerando o projeto Android (build.sh android_source)…"
  ./build.sh android_source
fi

# --- Localiza o projeto gerado (mais recente) ----------------------------------
GEN="$(ls -dt "$PROJ_ROOT"/android/target/*-android-source 2>/dev/null | head -1)"
[ -n "$GEN" ] && [ -d "$GEN" ] || { echo "ERRO: projeto gerado não encontrado. Rode com --gen primeiro."; exit 1; }
echo "==> Projeto: $GEN"
echo "==> compileSdk/targetSdk -> $SDK_TARGET | Gradle JVM -> $JDK17"

# --- (1) compileSdk/targetSdk para a plataforma estável ------------------------
sed -i '' -E "s/compileSdkVersion[[:space:]]+[0-9]+/compileSdkVersion $SDK_TARGET/" "$GEN/app/build.gradle"
sed -i '' -E "s/targetSdkVersion[[:space:]]+[0-9]+/targetSdkVersion $SDK_TARGET/"   "$GEN/app/build.gradle"

# --- (2) Gradle JVM = JDK 17 (sobrescreve o org.gradle.java.home gerado) --------
GP="$GEN/gradle.properties"
touch "$GP"
if grep -q '^org\.gradle\.java\.home=' "$GP"; then
  sed -i '' -E "s#^org\.gradle\.java\.home=.*#org.gradle.java.home=$JDK17#" "$GP"
else
  printf '\norg.gradle.java.home=%s\n' "$JDK17" >> "$GP"
fi

# --- (3) sdk.dir em local.properties -------------------------------------------
printf 'sdk.dir=%s\n' "$ANDROID_SDK" > "$GEN/local.properties"

# --- Modo IDE: só corrige e sai (abrir/rodar na IDE) ---------------------------
if [ "$PATCH_ONLY" == "1" ]; then
  echo "==> Projeto corrigido. Abra na IDE e dê Run (NÃO regere de dentro dela):"
  echo "    $GEN"
  exit 0
fi

# --- Build + install -----------------------------------------------------------
if ! "$ADB" devices | grep -qE '\bdevice$'; then
  echo "AVISO: nenhum emulador/dispositivo Android conectado (adb). Inicie o AVD antes."
fi
echo "==> ./gradlew installDebug"
( cd "$GEN" && ANDROID_HOME="$ANDROID_SDK" JAVA_HOME="$JDK17" ./gradlew --no-daemon installDebug )

# --- Launch --------------------------------------------------------------------
echo "==> Abrindo $PKG no emulador…"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
echo "==> Pronto."
