#!/bin/bash

LANGS=simple,la
DAT=../dat/


if [ "$(basename $(pwd))" != 'wikAPIdia-loader' ]; then
    echo "script must be executed from wikAPIdia-loader directory." >&2
    exit 1
fi


source ../wikAPIdia-utils/src/main/scripts/utils.sh || exit 1


mvn clean

(
    cd $WP_DOWNLOAD &&
    compile  &&
    execClass org.wikapidia.download.RequestedLinkGetter \
            -l $LANGS \
            -f articles,interlang_links,links \
            -o $DAT/dump_files.tsv &&
    execClass org.wikapidia.download.FileDownloader \
            -o $DAT/dump/ \
            $DAT/dump_files.tsv

) || exit 1

compile &&
execClass org.wikapidia.dao.load.DumpLoader -t -i $DAT/dump/*/*/*.articles.*.xml.bz2 &&
execClass org.wikapidia.dao.load.RedirectLoader -t -i -l $LANGS &&
execClass org.wikapidia.dao.load.ConceptLoader -t -i -l $LANGS -n monolingual &&
execClass org.wikapidia.dao.load.WikiTextDumpLoader -t -i -l $LANGS || exit 1
