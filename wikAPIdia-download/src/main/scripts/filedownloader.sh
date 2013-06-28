#!/bin/bash

source ../wikapidia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.download.FileDownloader $@ ||
die "$0 failed"
