#!/bin/bash

source ../wikapidia-utils/src/main/scripts/utils.sh &&
downloadpath=${WP_DOWNLOAD}/download/*/*/* &&
#die "SCRIPT NOT YET IMPLEMENTED" &&
compile  &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.RequestedLinkGetter -o links.tsv -l en -n articles) &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.FileDownloader -o download links.tsv) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.DumpLoader -ti ${downloadpath}) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.RedirectLoader -ti) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.WikiTextDumpLoader -ti) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.ConceptLoader -ti) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.UniversalLinkLoader -ti) ||
die "$0 failed"
