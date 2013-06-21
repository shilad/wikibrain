#!/bin/bash
#
# Fully process the input dump, running all loaders.
# Must be invoked from the wikiapidia-loader directory.


source ../wikapidia-utils/src/main/scripts/utils.sh &&
die "SCRIPT NOT YET IMPLEMENTED" &&
compile  &&
java -cp "$CLASSPATH" $JAVA_OPTS org.wikapidia.dao.load.DumpLoader "$@"  &&
java -cp "$CLASSPATH" $JAVA_OPTS org.wikapidia.dao.load.ConceptLoader "$@" ||
die "$0 failed"

# Order of existing classes:
# DumpLoader
# RedirectLoader
# LocalLinkLoader
# CategoryLoader
# ConceptLoader
# UniversalLinkLoader