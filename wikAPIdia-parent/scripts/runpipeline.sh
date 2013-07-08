#!/bin/bash


source ../wikapidia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.RequestedLinkGetter $@) &&
(cd ${WP_DOWNLOAD} && execClass org.wikapidia.download.FileDownloader $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.DumpLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.RedirectLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.WikiTextLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.ConceptLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.UniversalLinkLoader -ti $@) ||
die "$0 failed"
