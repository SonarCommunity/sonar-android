#!/bin/bash

set -euo pipefail
echo "Running with SQ=$SQ_VERSION"

cd its/plugin
mvn -Dsonar.runtimeVersion="$SQ_VERSION" -DjavaVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false test
