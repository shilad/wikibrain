#!/bin/bash
#
# Runs the ConceptLoader on the database.
# Unless otherwise specified, Concepts will be mapped with
# the monolingual algorithm.
# Must be invoked from the wikiapidia-loader directory.

source ../wikapidia-utils/src/main/scripts/utils.bash

compile

getRemoteClasspath

CLASS_PATH="${REMOTE_CLASS_PATH}:${LOCAL_CLASS_PATH}"

java -cp "$CLASS_PATH" $JVM_OPTS org.wikapidia.dao.load.ConceptLoader "$@"