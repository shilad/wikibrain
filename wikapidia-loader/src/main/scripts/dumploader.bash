#!/bin/bash
#
# Runs the DumpLoader on the input dump.
# Must be invoked from the wikiapidia-loader directory.

source ../wikapidia-utils/src/main/scripts/utils.bash

compile

getRemoteClasspath

CLASS_PATH="${REMOTE_CLASS_PATH}:${LOCAL_CLASS_PATH}"

java -cp "$CLASS_PATH" $JVM_OPTS org.wikapidia.dao.load.DumpLoader "$@"
