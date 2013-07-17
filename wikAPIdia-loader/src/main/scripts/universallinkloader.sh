#!/bin/bash
#
# Runs the UniversalLinkLoader on the database.
# Must be invoked from the wikiapidia-loader directory.


source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.dao.load.UniversalLinkLoader $@ ||
die "$0 failed"