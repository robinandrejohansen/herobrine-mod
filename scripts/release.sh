#!/usr/bin/env bash
#
# Cut a release. One argument: the new version.
#
#     scripts/release.sh 1.0.1
#
# This exists because "ship a bug fix" should be one command rather than six
# remembered steps, and the step everybody forgets is bumping mod_version in
# gradle.properties — which produces a jar named after the OLD version and a
# release that quietly ships the wrong file.
#
# The website needs nothing doing to it. It asks GitHub for the latest release
# on every page load, so the download button and the version in the install
# steps update themselves the moment this finishes.
set -euo pipefail

version="${1:?usage: scripts/release.sh <version>   e.g. 1.0.1}"
cd "$(dirname "$0")/.."

sed -i '' "s/^mod_version=.*/mod_version=${version}/" gradle.properties

# ./run.sh rather than ./gradlew, and this is not a style preference.
# Minecraft 26.2 needs Java 25, Homebrew installs its JDK keg-only so it is not
# on PATH, and run.sh is the thing that knows where it is. Calling gradlew
# directly fails with "release version 25 not supported" — after the version has
# already been bumped, which leaves the tree half-released.
./run.sh build

jar="build/libs/herobrine-${version}.jar"
[ -f "$jar" ] || { echo "expected $jar and it is not there"; exit 1; }

git add -A
# Only commit if there is something to commit. With set -e, a clean tree would
# otherwise abort the release between the build and the tag — which is the
# worst place to stop, since the version is already bumped.
git diff --cached --quiet || git commit -m "Release ${version}"
git push origin main

gh release create "v${version}" "$jar" \
  --title "v${version}" \
  --generate-notes

echo
echo "released v${version}"
echo "the site picks it up on its own — nothing to redeploy"
