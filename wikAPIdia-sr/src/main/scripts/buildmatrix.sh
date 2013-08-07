#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile  &&
execClass -Xmx6g org.wikapidia.sr.MatrixBuilder $@ ||
die "$0 failed"