#!/bin/sh
set -eu
cd "$(dirname "$0")"
./compile.sh
exec java -cp out com.orbitalix.app.Main "$@"
