#!/bin/bash

if [ -z "$1" ]; then
    echo "usage: $0 version" >&2
    exit 1
fi

new_version=$1
pattern="([0-9+]).([0-9])+.([0-9])+"

if ! [[ "$new_version" =~ $pattern ]]; then
    echo "version must be 0.majornum.minornum" >&2
    exit 1
fi

version1=${BASH_REMATCH[1]}
version2=${BASH_REMATCH[2]}
version3=${BASH_REMATCH[3]}
branch=release-${version1}.${version2}

ALL_JARS=`pwd`/.tmp/all-jars

old_version=$(grep version ./pom.xml | tail -1 | sed 's/.*<version>//' | sed 's/<\/version>//')
target=`pwd`/wikibrain-and-deps-${new_version}.zip

echo "current version is $old_version"
echo "new version will be $new_version"
echo "new branch will be $branch"
echo "new zipfile will be $target"
echo "if all these things seem correct, type 'go'"

read GO

if [ ${GO} != "go" ]; then
    echo "aborting!"
fi

rm -rf ${ALL_JARS} ${target}

# prepare release
git branch ${branch} &&
mvn clean &&
mvn versions:set -DnewVersion=${new_version} &&
mvn  -f ./wikibrain-parent/pom.xml versions:set -DnewVersion=${new_version} &&
mvn  -N versions:update-child-modules &&
mvn versions:commit ||
{ echo "updating versions failed" >&2; exit 1 }

# push release and commit
mvn clean compile package &&
git commit -m "Released ${new_version}" &&
mvn clean deploy -P release-version -DskipTests=true &&
mvn nexus-staging:release  -P release-version -DskipTests=true ||
{ echo "Pushing release to git or sonatype failed" >&2; exit 1 }

# zip up full jar
mvn dependency:copy-dependencies -DoutputDirectory=${ALL_JARS} &&
cp -p wikibrain*/target/wikibrain*${new_version}.jar ${ALL_JARS} &&
(cd ${ALL_JARS} && zip ${target} *.jar) &&
scp -p ${target} shilad.com:/var/www/html/www.shilad.com/wikibrain ||
{ echo "Uploading master zip failed" >&2; exit 1 }


