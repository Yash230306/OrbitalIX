#!/bin/sh
set -eu
mkdir -p out
find src/main/java -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d out
