#!/bin/bash
#
# Runs the ConceptLoader on the database.
# Unless otherwise specified, Concepts will be mapped with
# the monolingual algorithm.
# Must be invoked from the wikiapidia-loader directory.

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.dao.load.ConceptLoader $@ ||
die "$0 failed"