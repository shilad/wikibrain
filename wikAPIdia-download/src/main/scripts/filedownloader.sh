#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile &&
execClass org.wikapidia.download.FileDownloader $@ ||
die "$0 failed"
