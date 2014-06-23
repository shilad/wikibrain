#!/bin/bash

if [ -z "$1" ]; then
    echo "usage: $0 version" >&2
    exit 1
fi

new_version=$1
pattern="([0-9+]).([0-9])+-SNAPSHOT"

if ! [[ "$new_version" =~ $pattern ]]; then
    echo "version must be 0.majornum-SNAPSHOT" >&2
    exit 1
fi

version1=${BASH_REMATCH[1]}
version2=${BASH_REMATCH[2]}


old_version=$(grep version ./pom.xml | tail -1 | sed 's/.*<version>//' | sed 's/<\/version>//')

echo "current version is $old_version"
echo "new version will be $new_version"
echo "if all these things seem correct, type 'go'"

read GO

if [ ${GO} != "go" ]; then
    echo "aborting!"
fi

# prepare release
git checkout master &&
mvn clean &&
mvn versions:set -DnewVersion=${new_version} &&
mvn  -f ./wikibrain-parent/pom.xml versions:set -DnewVersion=${new_version} &&
mvn  -N versions:update-child-modules &&
mvn versions:commit ||
{ echo "updating versions failed" >&2; exit 1; }

# git commit
mvn clean compile package &&
git commit -m "Bumped snapshot to ${new_version}" ||
{ echo "Pushing release to git or sonatype failed" >&2; exit 1; }