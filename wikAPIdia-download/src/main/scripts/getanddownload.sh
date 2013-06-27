#!/bin/bash

source ../wikapidia-utils/src/main/scripts/utils.sh &&
die "SCRIPT NOT YET IMPLEMENTED" &&
compile  &&
execClass org.wikapidia.download.RequestedLinkGetter $@ &&
execClass org.wikapidia.download.FileDownloader $@ ||
die "$0 failed"

