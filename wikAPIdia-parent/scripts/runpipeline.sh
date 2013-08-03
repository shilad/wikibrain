#!/bin/bash

stage=
case "$1" in
    all|download)
        stage=1
        ;;
    dump)
        stage=2
        ;;
    wikitext)
        stage=3
        ;;
    concept)
        stage=4
        ;;
    phrases)
        stage=5
        ;;
    lucene)
        stage=6
        ;;
    *)
        echo "unknown stage: $1 (must be 'all', 'download', 'dump', 'wikitext', 'concept', or 'analysis')" >&2
        exit 1
esac

shift

source ../wikAPIdia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  || die "$0 failed"

if [ "$stage" -le 1 ]; then
    (cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.RequestedLinkGetter $@) &&
    (cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.FileDownloader $@) ||
        die "$0 failed"
fi

if [ "$stage" -le 2 ]; then
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.DumpLoader -d $@) &&
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.RedirectLoader -d $@) ||
        die "$0 failed"
fi

if [ "$stage" -le 3 ]; then
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.WikiTextLoader -d $@) ||
        die "$0 failed"
fi

if [ "$stage" -le 4 ]; then
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.ConceptLoader -d $@) &&
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.UniversalLinkLoader -d $@) ||
    die "$0 failed"
fi

if [ "$stage" -le 5 ]; then
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.PhraseLoader -p anchortext $@) ||
    die "$0 failed"
fi

if [ "$stage" -le 5 ]; then
    (cd ${WP_LOADER} && execClass org.wikapidia.dao.load.LuceneLoader -d $@) ||
    die "$0 failed"
fi
