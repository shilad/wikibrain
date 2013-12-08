#!/bin/bash

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
compile  &&
execClass -Xmx6g org.wikapidia.sr.evaluation.EvaluationMain $@ ||
die "$0 failed"