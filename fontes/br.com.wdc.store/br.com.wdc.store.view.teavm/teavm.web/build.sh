#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../../../work/frontend/teavm.web"
COMMONS_DIR="../teavm.commons"

# Option --full: install all dependency modules before building
if [[ "$1" == "--full" ]]; then
    echo "=== Installing dependency modules ==="
    FRAMEWORK_DIR="../../../br.com.wdc.framework"
    STORE_DIR="../.."

    JAVA_HOME=$JAVA25_HOME mvn -f "$FRAMEWORK_DIR/pom.xml" clean install -DskipTests -q
    JAVA_HOME=$JAVA25_HOME mvn -f "$STORE_DIR/pom.xml" clean install \
        -pl br.com.wdc.store.domain,br.com.wdc.store.persistence,:persistence.client,br.com.wdc.store.presentation \
        -DskipTests -q
    echo "=== Dependencies installed ==="
fi

# Always rebuild teavm.commons (shared VDom, Swc, interop). The installs clean first: an IDE that builds into the same
# target/ may have left class files with compile errors there, which TeaVM would translate as they are.
JAVA_HOME=$JAVA25_HOME mvn -f "$COMMONS_DIR/pom.xml" clean install -DskipTests -q

# Compile TeaVM (generates JS into $DEPLOY_DIR/js/ and copies filtered index.html via maven-resources-plugin)
# clean is required to force TeaVM recompilation when only dependency JARs changed
rm -f "$DEPLOY_DIR/index.html"
JAVA_HOME=$JAVA25_HOME mvn clean process-classes -DskipTests

# The Spectrum Web Components, bundled into one file next to app.js (js/swc-elements.js)
npm ci --no-audit --no-fund --loglevel=error
npm run --silent build:swc

# Copy static assets for the landing page and browser
cp src/main/webapp/context.html "$DEPLOY_DIR/context.html"
cp src/main/webapp/favicon.ico "$DEPLOY_DIR/favicon.ico"

echo "Deploy complete: $DEPLOY_DIR"
