#!/usr/bin/env bash
set -euo pipefail

# Builds the Java modules required by the RoboVM iOS app (mavenLocal)
# using the robovm-compat profile (Java 17, no preview features).
#
# RoboVM AOT-compiles Java bytecode to native ARM64 for iOS.
# The shared modules must be compiled with Java 17 (the highest version
# that RoboVM 2.3.24's Soot AOT compiler can handle).

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

echo "Building robovm-compat modules: $PL"
mvn -Probovm-compat -DskipTests clean install -pl "$PL" -am

echo "Done. Artifacts installed in ~/.m2/repository"
echo ""
echo "Now you can build the iOS app:"
echo "  cd br.com.wdc.shopping/br.com.wdc.shopping.view.robovm"
echo "  ./gradlew launchIPhoneSimulator   # Run on simulator"
echo "  ./gradlew launchIOSDevice          # Run on device"
echo "  ./gradlew createIPA                # Create IPA for distribution"
