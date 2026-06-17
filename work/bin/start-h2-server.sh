#!/bin/bash
# Starts the H2 database in TCP server mode.
#
# Use this instead of the embedded H2 to isolate the database heap from the
# application server heap — essential for accurate per-session memory measurements
# with MemoryPerSessionScenario.
#
# Usage:
#   ./start-h2-server.sh [data-dir]
#
# Default data-dir: ../data  (relative to this script = work/data)
#
# After starting, configure application.toml:
#   [database]
#   url = "jdbc:h2:tcp://localhost/./wedocode-shopping;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
#
# Stop with: Ctrl+C

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
PROJECT_ROOT="$( cd "$WORK_DIR/.." && pwd )"

DATA_DIR="${1:-$WORK_DIR/data}"
DATA_DIR="$( cd "$DATA_DIR" && pwd )"

# JAR locations (checked in priority order)
H2_JAR="${HOME}/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar"
BACKEND_JAR="$PROJECT_ROOT/fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar"

if [ ! -f "$H2_JAR" ]; then
    echo "ERROR: H2 JAR not found at $H2_JAR"
    echo "Run 'mvn dependency:resolve' to download it."
    exit 1
fi

# The H2 server needs the framework JAR on its classpath so it can resolve
# the TO_BASE64 custom function alias registered by H2JsonDialect.
# The fat backend JAR contains all framework classes.
if [ ! -f "$BACKEND_JAR" ]; then
    echo "WARNING: Backend JAR not found at $BACKEND_JAR"
    echo "The TO_BASE64 SQL alias will fail. Build first with: mvn package -DskipTests"
    CLASSPATH="$H2_JAR"
else
    CLASSPATH="$H2_JAR:$BACKEND_JAR"
fi

echo "╔══════════════════════════════════════════════════════╗"
echo "║          H2 TCP Server — WeDoCode Shopping           ║"
echo "╠══════════════════════════════════════════════════════╣"
printf "║  Data dir : %-42s ║\n" "$DATA_DIR"
printf "║  H2 JAR   : %-42s ║\n" "$(basename $H2_JAR)"
printf "║  Port     : %-42s ║\n" "9092 (default)"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  JDBC URL for application.toml:                      ║"
echo "║  jdbc:h2:tcp://localhost/./wedocode-shopping         ║"
echo "║  ;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE           ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""
echo "Press Ctrl+C to stop the H2 server."
echo ""

exec java -cp "$CLASSPATH" org.h2.tools.Server \
    -tcp \
    -tcpAllowOthers \
    -tcpPort 9092 \
    -baseDir "$DATA_DIR" \
    -ifNotExists
