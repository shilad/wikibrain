#!/bin/bash
#
# Invoke this using ./src/main/processdump.sh
#
# Import environment parameters from conf.sh
# Import helpers from utils.sh
#
# Compile
# Update CLASSPATH from the classpath file
# java org.wikapidia.dao.load.LoaderMain $@

source ../wikapidia-utils/src/main/scripts/utils.bash

echo sourced

compile

read < ./target/classpath REMOTE_CLASS_PATH

CLASS_PATH="${REMOTE_CLASS_PATH}:$LOCAL_CLASS_PATH"

echo "$CLASS_PATH"

java -cp "$CLASS_PATH" $JVM_OPTS org.wikapidia.dao.load.DumpLoader "$@"
java -cp "$CLASS_PATH" $JVM_OPTS org.wikapidia.dao.load.ConceptLoader "$@"