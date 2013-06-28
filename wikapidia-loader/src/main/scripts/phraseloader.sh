#!/bin/bash
#
# Runs the phrase loader.
# Must be invoked from the wikiapidia-loader directory.

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile &&
execClass -Dphrases.dao.=10 org.wikapidia.dao.load.PhraseLoader $@ ||
die "$0 failed"