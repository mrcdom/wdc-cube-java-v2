#!/bin/bash

# Constrói os artefatos web que o backend serve a partir de work/frontend/.
#
# Cada frontend já tem o seu build.sh, cada um em um módulo diferente e com
# pré-requisitos diferentes; este script só sabe onde eles estão, em que ordem
# chamá-los e o que fazer quando uma ferramenta não está instalada.
#
# Uso:
#   ./build-frontends.sh                      # todos os alvos disponíveis
#   ./build-frontends.sh react teavm.web      # só os alvos citados
#   ./build-frontends.sh --full               # instala antes os módulos Maven de que o TeaVM depende
#   ./build-frontends.sh --list               # mostra os alvos e o estado dos pré-requisitos
#   SOURCE_MAPS=true ./build-frontends.sh flutter
#
# Um alvo sem a ferramenta necessária é PULADO, não é erro: quem não tem Flutter
# instalado continua conseguindo gerar os outros três. O código de saída só é
# diferente de zero se algum build de fato falhar.

set -uo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
ROOT_DIR="$( cd "$WORK_DIR/.." && pwd )"
FONTES="$ROOT_DIR/fontes"
REMOTE="$FONTES/br.com.wdc.shopping/br.com.wdc.shopping.view.remote"
TEAVM="$FONTES/br.com.wdc.shopping/br.com.wdc.shopping.view.teavm"

# alvo | script de build | ferramenta exigida | pasta de destino em work/frontend
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

# ---------------------------------------------------------------- argumentos

FULL=false
LIST=false
SELECTED=()

while [ $# -gt 0 ]; do
    case "$1" in
        --full)  FULL=true ;;
        --list)  LIST=true ;;
        -h|--help)
            awk 'NR>2 && /^#/ { sub(/^# ?/, ""); print; next } NR>2 { exit }' "${BASH_SOURCE[0]}"
            exit 0
            ;;
        -*)
            echo "Opção desconhecida: $1" >&2
            exit 2
            ;;
        *)
            if entry_of "$1" >/dev/null; then
                SELECTED+=("$1")
            else
                echo "Alvo desconhecido: $1 (conhecidos: $(names | tr '\n' ' '))" >&2
                exit 2
            fi
            ;;
    esac
    shift
done

[ ${#SELECTED[@]} -eq 0 ] && while read -r n; do SELECTED+=("$n"); done < <(names)

# ------------------------------------------------------------- pré-requisitos

# Os builds TeaVM chamam `JAVA_HOME=$JAVA21_HOME mvn ...`; sem a variável o Maven
# rodaria com JAVA_HOME vazio.
if [ -z "${JAVA21_HOME:-}" ] && [ -n "${JAVA_HOME:-}" ]; then
    export JAVA21_HOME="$JAVA_HOME"
fi

missing_tool() {
    local tool="$1"
    case "$tool" in
        mvn)
            command -v mvn >/dev/null || { echo "mvn não encontrado no PATH"; return 0; }
            [ -n "${JAVA21_HOME:-}" ] || { echo "JAVA21_HOME (ou JAVA_HOME) não aponta para um JDK 21"; return 0; }
            ;;
        *)
            command -v "$tool" >/dev/null || { echo "$tool não encontrado no PATH"; return 0; }
            ;;
    esac
    return 1
}

if $LIST; then
    printf '%-12s  %-22s  %s\n' ALVO DESTINO PRÉ-REQUISITO
    for t in "${TARGETS[@]}"; do
        reason=$(missing_tool "$(field "$t" 3)") && state="indisponível — $reason" || state="ok ($(field "$t" 3))"
        printf '%-12s  %-22s  %s\n' "$(field "$t" 1)" "work/frontend/$(field "$t" 4)" "$state"
    done
    exit 0
fi

# ---------------------------------------------------------- dependências Maven

# --full replica o que os dois build.sh do TeaVM fazem com a sua própria opção
# --full, porém uma vez só: framework para o remote.shell.teavm e, para o
# teavm.web, também os módulos de domínio/persistência/apresentação.
if $FULL; then
    if reason=$(missing_tool mvn); then
        echo "--full precisa do Maven: $reason" >&2
        exit 2
    fi
    echo "=== Instalando módulos Maven (framework + shopping) ==="
    JAVA_HOME=$JAVA21_HOME mvn -f "$FONTES/br.com.wdc.framework/pom.xml" install -DskipTests -q || exit 1
    JAVA_HOME=$JAVA21_HOME mvn -f "$FONTES/br.com.wdc.shopping/pom.xml" install \
        -pl br.com.wdc.shopping.domain,br.com.wdc.shopping.persistence,:persistence.client,br.com.wdc.shopping.presentation \
        -DskipTests -q || exit 1
fi

# ------------------------------------------------------------------- execução

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
        echo "PULADO: $reason"
        RESULTS+=("PULADO   $name  ($reason)")
        continue
    fi

    if [ ! -x "$script" ]; then
        echo "PULADO: script não encontrado ou sem permissão de execução: $script"
        RESULTS+=("PULADO   $name  (build.sh ausente)")
        continue
    fi

    started=$SECONDS
    if "$script"; then
        RESULTS+=("OK       $name  ($((SECONDS - started))s)")
    else
        RESULTS+=("FALHOU   $name  ($((SECONDS - started))s)")
        FAILED=1
    fi
done

echo ""
echo "=================================================="
echo "Resumo"
echo "=================================================="
printf '%s\n' "${RESULTS[@]}"
echo ""
echo "Destino: $WORK_DIR/frontend"
echo "Sirva com: $SCRIPT_DIR/start-server.sh"

exit $FAILED
