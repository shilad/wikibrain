#!/bin/bash
#
# Runs the DumpLoader on the input dump.
# Must be invoked from the wikiapidia-loader directory.

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.dao.load.DumpLoader $@ ||
die "$0 failed"
