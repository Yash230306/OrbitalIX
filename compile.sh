#!/usr/bin/env sh
cd "$(dirname "$0")"
mkdir -p out
find src -name "*.java" | while IFS= read -r file; do
  printf '"%s"\n' "$file"
done > sources.txt
javac --release 17 -encoding UTF-8 -d out @sources.txt
echo "Compiled OK"
