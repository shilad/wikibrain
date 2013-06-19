#!/bin/sh
# This bash script contains common shell functions and is included by all bash scripts
#

function die() {
    echo $1 >&2
    exit 1
}

function compile() {
    echo compiling...
    (cd ../wikAPIdia-parent &&
    mvn compile || die "compilation failed")
    echo compiled
}

function getRemoteClasspath() {
    read < ./target/localclasspath.txt REMOTE_CLASS_PATH
}
# source all util scripts here, so that other scripts only
# need to source this script to source everything
source ../wikapidia-utils/src/main/scripts/conf.bash

