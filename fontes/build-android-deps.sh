#!/usr/bin/env bash
set -euo pipefail

# Builds the Java modules required by the Android app (mavenLocal)
# using the android-compat profile (Java 22, no preview features).

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

MODULES=(
  br.com.wdc.framework/br.com.wdc.framework.commons
  br.com.wdc.framework/br.com.wdc.framework.cube
  br.com.wdc.shopping/br.com.wdc.shopping.domain
  br.com.wdc.shopping/br.com.wdc.shopping.presentation
  br.com.wdc.shopping/br.com.wdc.shopping.api-client
)

PL=$(IFS=,; echo "${MODULES[*]}")

echo "Building android-compat modules: $PL"
mvn -Pandroid-compat -DskipTests clean install -pl "$PL" -am

echo "Done. Artifacts installed in ~/.m2/repository"
