#!/bin/bash
set -e
cd "$(dirname "$0")"

DEPLOY_DIR="../../br.com.wdc.shopping.backend/work/frontend/app.teavm"

# Compile TeaVM (generates JS into $DEPLOY_DIR/js/)
JAVA_HOME=$JAVA21_HOME mvn process-classes -DskipTests

# Copy webapp assets (index.html) to deploy dir
cp src/main/webapp/index.html "$DEPLOY_DIR/"

echo "Deploy complete: $DEPLOY_DIR"
