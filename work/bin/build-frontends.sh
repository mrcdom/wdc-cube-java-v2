#!/bin/bash

# Builds the web artifacts the backend serves from work/frontend/.
#
# Each frontend already has its own build.sh, each in a different module and with
# different prerequisites; this script only knows where they are, in which order
# to call them and what to do when a tool is not installed.
#
# Usage:
#   ./build-frontends.sh                      # all available targets
#   ./build-frontends.sh react teavm.web      # only the named targets
#   ./build-frontends.sh --skip-install       # does not reinstall the Maven modules before TeaVM
#   ./build-frontends.sh --list               # shows the targets and the state of their prerequisites
#   SOURCE_MAPS=true ./build-frontends.sh flutter
#
# A target without the required tool is SKIPPED, not an error: someone without Flutter
# installed can still build the other three. The exit code is non-zero only
# if some build actually fails.
#
# Both TeaVM builds compile against the JARs in ~/.m2, not against the reactor: without
# reinstalling the modules first, they silently produce an app with the previous
# version of the domain — the app opens and fails only on the call that changed. That
# is why installing is the default, and skipping has to be requested.

set -uo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
ROOT_DIR="$( cd "$WORK_DIR/.." && pwd )"
FONTES="$ROOT_DIR/fontes"
REMOTE="$FONTES/br.com.wdc.store/br.com.wdc.store.view.remote"
TEAVM="$FONTES/br.com.wdc.store/br.com.wdc.store.view.teavm"

# target | build script | required tool | destination folder in work/frontend
TARGETS=(
    "react|$REMOTE/remote.shell.react/build.sh|npm|remote.shell.react"
    "flutter|$REMOTE/remote.shell.flutter/flutter.web/build.sh|flutter|remote.shell.flutter"
    "teavm.shell|$REMOTE/remote.shell.teavm/build.sh|mvn|remote.shell.teavm"
    "teavm.web|$TEAVM/teavm.web/build.sh|mvn|teavm.web"
)

field() { echo "$1" | cut -d'|' -f"$2"; }

names() { for t in "${TARGETS[@]}"; do field "$t" 1; done; }

entry_of() {
    for t in "${TARGETS[@]}"; do
        [ "$(field "$t" 1)" = "$1" ] && { echo "$t"; return 0; }
    done
    return 1
}

# ----------------------------------------------------------------- arguments

SKIP_INSTALL=false
LIST=false
SELECTED=()

while [ $# -gt 0 ]; do
    case "$1" in
        --skip-install) SKIP_INSTALL=true ;;
        --list)  LIST=true ;;
        -h|--help)
            awk 'NR>2 && /^#/ { sub(/^# ?/, ""); print; next } NR>2 { exit }' "${BASH_SOURCE[0]}"
            exit 0
            ;;
        -*)
            echo "Unknown option: $1" >&2
            exit 2
            ;;
        *)
            if entry_of "$1" >/dev/null; then
                SELECTED+=("$1")
            else
                echo "Unknown target: $1 (known: $(names | tr '\n' ' '))" >&2
                exit 2
            fi
            ;;
    esac
    shift
done

[ ${#SELECTED[@]} -eq 0 ] && while read -r n; do SELECTED+=("$n"); done < <(names)

# -------------------------------------------------------------- prerequisites

# The TeaVM builds call `JAVA_HOME=$JAVA25_HOME mvn ...`; without the variable Maven
# would run with an empty JAVA_HOME.
if [ -z "${JAVA25_HOME:-}" ] && [ -n "${JAVA_HOME:-}" ]; then
    export JAVA25_HOME="$JAVA_HOME"
fi

missing_tool() {
    local tool="$1"
    case "$tool" in
        mvn)
            command -v mvn >/dev/null || { echo "mvn not found in PATH"; return 0; }
            [ -n "${JAVA25_HOME:-}" ] || { echo "JAVA25_HOME (or JAVA_HOME) does not point to a JDK 25"; return 0; }
            ;;
        *)
            command -v "$tool" >/dev/null || { echo "$tool not found in PATH"; return 0; }
            ;;
    esac
    return 1
}

if $LIST; then
    printf '%-12s  %-22s  %s\n' TARGET DESTINATION PREREQUISITE
    for t in "${TARGETS[@]}"; do
        reason=$(missing_tool "$(field "$t" 3)") && state="unavailable — $reason" || state="ok ($(field "$t" 3))"
        printf '%-12s  %-22s  %s\n' "$(field "$t" 1)" "work/frontend/$(field "$t" 4)" "$state"
    done
    exit 0
fi

# ---------------------------------------------------------- Maven dependencies

# Does, just once, what the two TeaVM build.sh scripts each do with their
# --full option: the framework for remote.shell.teavm and, for teavm.web, also
# domain/persistence/presentation. Runs only if some TeaVM target was requested.
needs_maven_modules=false
for name in "${SELECTED[@]}"; do
    case "$name" in teavm.*) needs_maven_modules=true ;; esac
done

if $needs_maven_modules && ! $SKIP_INSTALL; then
    if reason=$(missing_tool mvn); then
        echo "Maven module installation skipped: $reason"
    else
        echo "=== Installing Maven modules (framework + store) ==="
        echo "    Without this TeaVM would compile against the previous ~/.m2 — use --skip-install if you know it is up to date."
        # clean: an IDE that builds into the same target/ may have left class files with compile errors there
        JAVA_HOME=$JAVA25_HOME mvn -f "$FONTES/br.com.wdc.framework/pom.xml" clean install -DskipTests -q || exit 1
        JAVA_HOME=$JAVA25_HOME mvn -f "$FONTES/br.com.wdc.store/pom.xml" clean install \
            -pl br.com.wdc.store.domain,br.com.wdc.store.persistence,:persistence.client,br.com.wdc.store.presentation \
            -DskipTests -q || exit 1
    fi
elif $needs_maven_modules; then
    echo "=== --skip-install: TeaVM will use the JARs already in ~/.m2 ==="
fi

# ------------------------------------------------------------------ execution

RESULTS=()
FAILED=0

for name in "${SELECTED[@]}"; do
    entry=$(entry_of "$name")
    script=$(field "$entry" 2)
    tool=$(field "$entry" 3)
    dest=$(field "$entry" 4)

    echo ""
    echo "=================================================="
    echo "$name  →  work/frontend/$dest"
    echo "=================================================="

    if reason=$(missing_tool "$tool"); then
        echo "SKIPPED: $reason"
        RESULTS+=("SKIPPED  $name  ($reason)")
        continue
    fi

    if [ ! -x "$script" ]; then
        echo "SKIPPED: script not found or not executable: $script"
        RESULTS+=("SKIPPED  $name  (build.sh missing)")
        continue
    fi

    started=$SECONDS
    if "$script"; then
        RESULTS+=("OK       $name  ($((SECONDS - started))s)")
    else
        RESULTS+=("FAILED   $name  ($((SECONDS - started))s)")
        FAILED=1
    fi
done

echo ""
echo "=================================================="
echo "Summary"
echo "=================================================="
printf '%s\n' "${RESULTS[@]}"
echo ""
echo "Destination: $WORK_DIR/frontend"
echo "Serve with: $SCRIPT_DIR/start-server.sh"

exit $FAILED
