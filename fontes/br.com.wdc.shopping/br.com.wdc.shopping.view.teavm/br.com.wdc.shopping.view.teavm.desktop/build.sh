#!/bin/bash
# Build the WDC Shopping Desktop app using Tauri
# Prerequisites: Rust toolchain, cargo tauri CLI (cargo install tauri-cli)

cd "$(dirname "$0")"

# 1. Build the web subproject first (Java → JS)
echo ">>> Building TeaVM web subproject..."
cd ../br.com.wdc.shopping.view.teavm.web
JAVA_HOME=$JAVA21_HOME mvn process-classes -DskipTests -q
if [ $? -ne 0 ]; then
    echo "ERROR: Web subproject build failed"
    exit 1
fi

# 2. Build the Tauri desktop app
echo ">>> Building Tauri desktop app..."
cd ../br.com.wdc.shopping.view.teavm.desktop
cargo tauri build
