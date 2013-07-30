#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.dao.load.LuceneLoader $@ ||
die "$0 failed"