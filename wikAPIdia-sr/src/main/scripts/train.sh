#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile  &&
execClass org.wikapidia.sr.MetricTrainer $@ ||
die "$0 failed"
