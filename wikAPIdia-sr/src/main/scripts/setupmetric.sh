#!/bin/bash

localmetrics=
universalmetrics=
case "$1" in
    all)
        localmetrics=("ESA" "LocalMilneWitten")
        universalmetrics=("UniversalMilneWitten")
        ;;
    ESA)
        localmetrics=("ESA")
        ;;
    LocalMilneWitten)
        localmetrics=("LocalMilneWitten")
        ;;
    UniversalMilneWitten)
        universalmetrics=("UniversalMilneWitten")
        ;;
    *)
        echo "unknown metric: $1"
        exit 1
esac

shift

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile || die "$0 failed"

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