#!/usr/bin/env sh
cd "$(dirname "$0")"
java -cp out com.orbitalix.app.Main "$@"
