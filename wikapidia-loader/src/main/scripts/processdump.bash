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

source ../wikapidia-utils/src/main/scripts/*

echo "sourced"

compile

java $JVM_OPTS org.wikapidia.dao.load.DumpLoader $@
java $JVM_OPTS org.wikapidia.dao.load.ConceptLoader $@