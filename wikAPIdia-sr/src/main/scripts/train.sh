#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  &&
execClass org.wikapidia.sr.MetricTrainer || exit 1