#!/bin/bash

localmetrics=("ESA" "milnewitten")
universalmetrics=("UniversalMilneWitten")


source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile || die "$0 failed"
if [ "$1" == "all" ]; then
    shift
    for m in "${localmetrics[@]}"
    do
        (echo $m) &&
        (execClass -Xmx6g org.wikapidia.sr.MatrixBuilder -m $m $@) &&
        (execClass org.wikapidia.sr.MetricTrainer -m $m $@) ||
            die "$0 failed"
    done

    for u in "${universalmetrics[@]}"
    do
        (echo $m) &&
        (execClass -Xmx6g org.wikapidia.sr.MatrixBuilder -u $u $@) &&
        (execClass org.wikapidia.sr.MetricTrainer -u $u $@) ||
            die "$0 failed"
    done
else
    (execClass -Xmx6g org.wikapidia.sr.MatrixBuilder $@) &&
    (execClass org.wikapidia.sr.MetricTrainer $@) ||
        die "$0 failed"
fi