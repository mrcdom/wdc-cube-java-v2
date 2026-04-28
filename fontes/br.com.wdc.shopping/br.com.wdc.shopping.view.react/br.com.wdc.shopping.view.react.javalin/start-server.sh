#!/bin/bash

# Startup script for WeDoCode Shopping React Javalin Server
# Usage: ./start-server.sh [port]

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."

# Ensure JAVA_HOME is set correctly
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-26.jdk/Contents/Home
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
JAR_FILE="$SCRIPT_DIR/target/br.com.wdc.shopping.view.react.javalin-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR not found: $JAR_FILE"
    echo "Building project..."
    cd "$PROJECT_ROOT" && mvn package -DskipTests -q
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
exec java -jar "$JAR_FILE" "$PORT"
