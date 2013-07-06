#!/bin/bash


source ../wikapidia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.RequestedLinkGetter $@) &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.FileDownloader $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.DumpLoader -d $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.RedirectLoader -d $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.WikiTextLoader -d $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.ConceptLoader -d $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.UniversalLinkLoader -d $@) ||
die "$0 failed"
