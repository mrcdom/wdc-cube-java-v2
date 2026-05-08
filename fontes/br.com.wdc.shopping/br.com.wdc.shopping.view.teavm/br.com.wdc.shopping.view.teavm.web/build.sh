#!/bin/bash
cd "$(dirname "$0")"
JAVA_HOME=$JAVA21_HOME mvn process-classes -DskipTests
