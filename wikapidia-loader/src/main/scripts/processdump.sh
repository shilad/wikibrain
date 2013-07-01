#!/bin/bash
#
# Fully process the input dump, running all loaders.
# Must be invoked from the wikiapidia-loader directory.


source ../wikapidia-utils/src/main/scripts/utils.sh &&
mvn clean &&
compile  &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.DumpLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.RedirectLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.WikiTextLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.ConceptLoader -ti $@) &&
(cd ${WP_LOADER} && execClass org.wikapidia.dao.load.UniversalLinkLoader -ti $@) ||
die "$0 failed"

# Order of existing classes:
# DumpLoader
# RedirectLoader
# WikiTextLoader
# ConceptLoader
# UniversalLinkLoader
#
# List of all params:
# c - config file
# t - beginLoad
# i - endLoad
# l - language or language set or ILLs (basically language set)
# n - concept mapping algorithm