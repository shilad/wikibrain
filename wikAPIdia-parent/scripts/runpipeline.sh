#!/bin/bash

source ../wikapidia-utils/src/main/scripts/utils.sh &&
die "SCRIPT NOT YET IMPLEMENTED" &&
compile  &&
execClass org.wikapidia.download.RequestedLinkGetter $@ &&
execClass org.wikapidia.download.FileDownloader $@ &&
execClass org.wikapidia.dao.load.DumpLoader $@ &&
execClass org.wikapidia.dao.load.RedirectLoader $@ &&
execClass org.wikapidia.dao.load.WikiTextDumpLoader $@ &&
execClass org.wikapidia.dao.load.ConceptLoader $@ &&
execClass org.wikapidia.dao.load.UniversalLinkLoader $@ ||
die "$0 failed"
