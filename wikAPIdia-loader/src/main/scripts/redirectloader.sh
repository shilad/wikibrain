#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.dao.load.RedirectLoader $@ ||
die "$0 failed"