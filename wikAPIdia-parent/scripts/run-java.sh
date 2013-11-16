#!/bin/bash

if [ -z "$1" ]; then
    echo "usage: $0 package.and.Class arg1 arg2 ...."
    exit 1
fi

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  || die "$0 failed"

cd ${WP_LOADER} && execClass $@
