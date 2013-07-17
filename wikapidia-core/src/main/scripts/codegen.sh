#!/bin/sh
#
# Runs the function to generate the jOOQ files
# Requires no parameters, but must be run from the home directory
# of any module.

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compileJooq ||
die "$0 failed"