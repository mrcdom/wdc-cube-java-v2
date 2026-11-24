#!/bin/bash

# Startup script for the WDC Store Javalin server
# Usage: ./start-server.sh [port]

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
PROJECT_ROOT="$( cd "$WORK_DIR/.." && pwd )"

# Work dir is the parent of this script (work/) —
# config/application.toml resolves relative to it
cd "$WORK_DIR"

# JDK 25: JAVA25_HOME takes precedence over the session's JAVA_HOME
export JAVA_HOME="${JAVA25_HOME:-$JAVA_HOME}"
if [ -z "$JAVA_HOME" ]; then
    echo "Error: set JAVA25_HOME (or JAVA_HOME) to a Java 25 JDK path."
    echo "  Example: export JAVA25_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    exit 1
fi
JAVA="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA" ]; then
    echo "Error: $JAVA not found. Check JAVA25_HOME/JAVA_HOME."
    exit 1
fi

# Check Java version
JAVA_VERSION=$("$JAVA" -version 2>&1 | head -1)
echo "Using: $JAVA_VERSION"

# Ensure JAR exists
JAR_FILE="$PROJECT_ROOT/fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR not found: $JAR_FILE"
    echo "Building project..."
    cd "$PROJECT_ROOT/fontes" && mvn package -DskipTests -q
    cd "$WORK_DIR"
fi

# Get port from argument or environment variable or default
PORT="${1:-${SERVER_PORT:-8080}}"

echo ""
echo "=================================================="
echo "WDC Store - Javalin Server"
echo "=================================================="
echo "Port: $PORT"
echo "JAR: $JAR_FILE"
echo ""
echo "Server will be available at: http://localhost:$PORT"
echo "Press Ctrl+C to stop"
echo "=================================================="
echo ""

# Run the server
# JVM tuning for load test / production (10 GB heap, ~10000 sessions):
#   -Xms4g -Xmx10g
#   -XX:+UseZGC                      # generational (the only mode since JDK 24); pauses < 1 ms, ideal for WebSocket
#   -XX:SoftMaxHeapSize=9g           # ZGC stays below 9 GB; 1 GB of headroom for spikes
#   -XX:ZCollectionInterval=5        # predictive GC: ZGC collects every 5 s even without pressure
#                                    # avoids memory spikes during low-load periods
#   -XX:MaxMetaspaceSize=512m
#   -XX:ReservedCodeCacheSize=256m
exec "$JAVA" -jar "$JAR_FILE" "$PORT"
