#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.download.RequestedLinkGetter $@ ||
die "$0 failed"
