#!/bin/bash
# This bash script contains common shell functions and is included by all bash scripts
#

function die() {
    echo $1 >&2
    exit 1
}

function compile() {
    echo compiling... &&
    (cd ../wikAPIdia-parent && mvn compile ) ||
    die "compilation failed"

    echo finished compiling
}

function execClass() {
    class=$1
    shift

    localclasspathfile=./target/localclasspath.txt
    if ! [ -f $localclasspathfile ]; then
        die "missing local classpath file $localclasspathfile"
    fi
    read < $localclasspathfile REMOTE_CLASSPATH
    CMD="java -cp \"${REMOTE_CLASSPATH}:${LOCAL_CLASSPATH}\" $JAVA_OPTS $class $@"
    $CMD ||
    die "executing '$CMD' failed"
}

# source all util scripts here, so that other scripts only
# need to source this script to source everything
source ../wikapidia-utils/src/main/scripts/conf.sh


