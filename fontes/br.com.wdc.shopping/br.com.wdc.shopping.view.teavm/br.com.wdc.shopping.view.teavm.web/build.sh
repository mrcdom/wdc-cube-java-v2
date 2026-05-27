#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../br.com.wdc.shopping.backend/work/frontend/app.teavm"

# Option --full: install all dependency modules before building
if [[ "$1" == "--full" ]]; then
    echo "=== Installing dependency modules ==="
    FRAMEWORK_DIR="../../../br.com.wdc.framework"
    SHOPPING_DIR="../.."

    JAVA_HOME=$JAVA21_HOME mvn -f "$FRAMEWORK_DIR/pom.xml" install -DskipTests -q
    JAVA_HOME=$JAVA21_HOME mvn -f "$SHOPPING_DIR/pom.xml" install \
        -pl br.com.wdc.shopping.domain,br.com.wdc.shopping.persistence,br.com.wdc.shopping.persistence.client,br.com.wdc.shopping.presentation \
        -DskipTests -q
    echo "=== Dependencies installed ==="
fi

# Compile TeaVM (generates JS into $DEPLOY_DIR/js/ and copies filtered index.html via maven-resources-plugin)
# clean is required to force TeaVM recompilation when only dependency JARs changed
JAVA_HOME=$JAVA21_HOME mvn clean process-classes -DskipTests

# Copy static assets for the landing page and browser
cp src/main/webapp/context.html "$DEPLOY_DIR/context.html"
cp src/main/webapp/favicon.ico "$DEPLOY_DIR/favicon.ico"

echo "Deploy complete: $DEPLOY_DIR"
