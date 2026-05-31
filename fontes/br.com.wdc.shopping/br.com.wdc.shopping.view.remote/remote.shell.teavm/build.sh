#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../../../work/frontend/remote.shell.teavm"
COMMONS_DIR="../../br.com.wdc.shopping.view.teavm/teavm.commons"

# Option --full: install all dependency modules before building
if [[ "$1" == "--full" ]]; then
    echo "=== Installing dependency modules ==="
    FRAMEWORK_DIR="../../../br.com.wdc.framework"

    JAVA_HOME=$JAVA21_HOME mvn -f "$FRAMEWORK_DIR/pom.xml" install -DskipTests -q
    echo "=== Dependencies installed ==="
fi

# Always rebuild teavm.commons (shared VDom, Swc, interop)
JAVA_HOME=$JAVA21_HOME mvn -f "$COMMONS_DIR/pom.xml" install -DskipTests -q

# Compile TeaVM (generates JS into $DEPLOY_DIR/js/ and copies webapp via maven-resources-plugin)
# clean is required to force TeaVM recompilation when only dependency JARs changed
JAVA_HOME=$JAVA21_HOME mvn clean process-classes -DskipTests

echo "Deploy complete: $DEPLOY_DIR"
