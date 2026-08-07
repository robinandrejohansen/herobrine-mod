#!/usr/bin/env bash
# Launch a development Minecraft client with this mod loaded.
#
#   ./run.sh          launch the game
#   ./run.sh build    just compile
#
# Minecraft 26.2 needs Java 25 or newer. Homebrew installs its JDK keg-only
# (not on PATH), so JAVA_HOME is set here rather than relying on the shell.
set -euo pipefail
cd "$(dirname "$0")"

for candidate in \
  /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
  /usr/libexec/java_home
do
  if [ -x "$candidate/bin/java" ]; then
    export JAVA_HOME="$candidate"
    break
  fi
done

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "No JDK 25+ found. Install one with:  brew install openjdk" >&2
  exit 1
fi

echo "Using $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
exec ./gradlew "${1:-runClient}" --console=plain
