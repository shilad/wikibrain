#!/bin/bash
# This bash script contains common shell functions and is included by all bash scripts
#

function die() {
    echo $1 >&2
    exit 1
}

function compile() {
    (cd ../wikAPIdia-parent &&
    mvn compile || die "compilation failed")
    echo compiling
}
