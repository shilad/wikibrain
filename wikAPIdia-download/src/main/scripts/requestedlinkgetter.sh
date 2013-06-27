#!/bin/bash

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.download.RequestedLinkGetter $@ ||
die "$0 failed"
