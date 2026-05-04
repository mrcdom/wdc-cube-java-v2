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

# ── Build robovm-rt-ide.jar ────────────────────────────────────────
# Eclipse cannot handle robovm-rt.jar because it contains java.lang.*,
# javax.*, sun.* etc. that conflict with the modular JDK (split-package).
# We create a stripped copy that keeps only org.robovm.* packages, which
# is all Eclipse needs for code-assist and compilation.
ROBOVM_VERSION="2.3.24"
RT_JAR="$HOME/.m2/repository/com/mobidevelop/robovm/robovm-rt/$ROBOVM_VERSION/robovm-rt-$ROBOVM_VERSION.jar"
IDE_GROUP="com.mobidevelop.robovm"
IDE_ARTIFACT="robovm-rt-ide"
IDE_JAR_DIR="$HOME/.m2/repository/$(echo $IDE_GROUP | tr '.' '/')/$IDE_ARTIFACT/$ROBOVM_VERSION"

echo ""
echo "Creating robovm-rt-ide.jar (stripped for Eclipse)..."
TMPDIR_IDE=$(mktemp -d)
(cd "$TMPDIR_IDE" && jar xf "$RT_JAR" org/ META-INF/)
mkdir -p "$IDE_JAR_DIR"
jar cf "$IDE_JAR_DIR/$IDE_ARTIFACT-$ROBOVM_VERSION.jar" -C "$TMPDIR_IDE" .
rm -rf "$TMPDIR_IDE"

# Install a minimal POM so Maven can resolve the dependency
cat > "$IDE_JAR_DIR/$IDE_ARTIFACT-$ROBOVM_VERSION.pom" <<EOFPOM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$IDE_GROUP</groupId>
  <artifactId>$IDE_ARTIFACT</artifactId>
  <version>$ROBOVM_VERSION</version>
  <description>Stripped robovm-rt for IDE use (no java.*/javax.*/sun.* packages)</description>
</project>
EOFPOM

echo "Installed $IDE_ARTIFACT-$ROBOVM_VERSION.jar → $IDE_JAR_DIR"
echo ""
echo "Now you can build the iOS app:"
echo "  cd br.com.wdc.shopping/br.com.wdc.shopping.view.robovm"
echo "  ./gradlew launchIPhoneSimulator   # Run on simulator"
echo "  ./gradlew launchIOSDevice          # Run on device"
echo "  ./gradlew createIPA                # Create IPA for distribution"
