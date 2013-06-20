#!/bin/sh
#
# Runs the function to generate the jOOQ files
# Requires no parameters, but must be run from the home directory
# of any module.

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile ||
die "$0 failed"