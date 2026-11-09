#!/bin/sh
set -eu
./compile.sh
exec java -cp out com.orbitalix.app.Main "$@"
