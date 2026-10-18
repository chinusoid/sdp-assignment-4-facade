#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build src/com/company/*.java
if [ "${1:-}" = "test" ]; then
    javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build -d build tests/com/company/*.java
    java -cp build com.company.FacadeTests
else
    java -cp build com.company.Main
fi
