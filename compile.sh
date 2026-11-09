#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -d out
echo "Compiled OK"
