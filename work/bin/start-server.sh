#!/bin/bash

# Startup script for WeDoCode Shopping React Javalin Server
# Usage: ./start-server.sh [port]

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
PROJECT_ROOT="$( cd "$WORK_DIR/.." && pwd )"

# Work dir is the parent of this script (work/) —
# config/application.toml resolves relative to it
cd "$WORK_DIR"

# Ensure JAVA_HOME is set correctly (must point to a Java 21 JDK)
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA21_HOME
    if [ -z "$JAVA_HOME" ]; then
        echo "Warning: JAVA_HOME not set. Please set it to a Java 21 JDK path."
        echo "  Example: export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
        exit 1
    fi
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please set JAVA_HOME correctly."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1)
echo "Using: $JAVA_VERSION"

# Ensure JAR exists
JAR_FILE="$PROJECT_ROOT/fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/target/br.com.wdc.shopping.backend-1.0.0.jar"
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
echo "WeDoCode Shopping React - Javalin Server"
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
#   -XX:+UseZGC -XX:+ZGenerational   # pausas < 1 ms, ideal para WebSocket
#   -XX:SoftMaxHeapSize=9g           # ZGC mantém abaixo de 9 GB; 1 GB de buffer para picos
#   -XX:ZCollectionInterval=5        # GC preditivo: ZGC coleta a cada 5 s mesmo sem pressão
#                                    # evita picos de memória em períodos de baixa carga
#   -XX:MaxMetaspaceSize=512m
#   -XX:ReservedCodeCacheSize=256m
exec java -jar "$JAR_FILE" "$PORT"
