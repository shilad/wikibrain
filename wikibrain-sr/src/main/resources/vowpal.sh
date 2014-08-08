#!/bin/bash

if [ $# -lt 1 ]; then
    echo "usage: $0 {encode|decode|build} ...." >&2
    exit 1
fi
action=$1
shift


if [ "$action" == "encode" ]; then
    if [ $# -lt 1 ]; then
        echo "usage: $0 encode jvm_mbs ...." >&2
        exit 1
    fi

    jvm_mbs=$1
    shift
    args=(${@// /\\ })

    export MAVEN_OPTS="-Xmx${jvm_mbs}M -ea"
    mvn compile &&
    mvn exec:java \
        -D exec.mainClass="edu.macalester.wpsemsim.topics.VowpalTranslator" \
        -D exec.classpathScope=runtime  \
        -D exec.args="encode ${args[*]}"
elif [ "$action"  == "build" ]; then
    if [ $# -ne 2 ]; then
        echo "usage: $0 build vw_dir rank" >&2
        exit 1
    fi
    VW=./bin/vowpal_wabbit/vowpalwabbit/vw
    if ! [[ -x $VW ]]; then
        echo "vowpal binary expected at $VW not found." >&2
        exit 1
    fi
    dir=$1
    rank=$2
    if ! [[ -d $dir ]]; then
        echo "vw input dir $dir not found." >&2
        exit 1
    fi
    if ! [[ -f "$dir/input.vw" ]]; then
        echo "vw input file $dir/input.vw not found." >&2
        exit 1
    fi
    $VW --lda $rank \
        --lda_alpha 0.1 \
        --lda_rho 0.1 \
        --lda_D 4000000 \
         --minibatch 100000 \
        --power_t 0.5 \
        --initial_t 1 \
        --passes 2 \
        --cache_file $dir/cache.vw \
        -d $dir/input.vw \
        -p $dir/articles.vw \
        --readable_model $dir/topics.vw \
        -b 19
        #-b 22
elif [[ "$action" == "decode" ]]; then
    if [ $# -lt 1 ]; then
        echo "usage: $0 encode jvm_mbs ...." >&2
        exit 1
    fi

    jvm_mbs=$1
    shift
    args=(${@// /\\ })

    export MAVEN_OPTS="-Xmx${jvm_mbs}M -ea"
    mvn compile &&
    mvn exec:java \
        -D exec.mainClass="edu.macalester.wpsemsim.topics.VowpalTranslator" \
        -D exec.classpathScope=runtime  \
        -D exec.args="decode ${args[*]}"

else
    echo "unknown action: $action" >&2
    exit 1
fi