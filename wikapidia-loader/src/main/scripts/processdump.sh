#!/bin/bash
#
# Fully process the input dump, running all loaders.
# Must be invoked from the wikiapidia-loader directory.


source ../wikapidia-utils/src/main/scripts/utils.sh &&
die "SCRIPT NOT YET IMPLEMENTED" &&
compile  &&
execClass org.wikapidia.dao.load.DumpLoader $@ &&
execClass org.wikapidia.dao.load.RedirectLoader $@ &&
execClass org.wikapidia.dao.load.WikiTextDumpLoader $@ &&
execClass org.wikapidia.dao.load.ConceptLoader $@ &&
execClass org.wikapidia.dao.load.UniversalLinkLoader $@ ||
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