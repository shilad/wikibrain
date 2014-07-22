#!/bin/bash

BASE=/Volumes/ShiladsFastDrive/wikibrain-en/dat/spatial/earth/

rm -f /tmp/naturalEarth*.zip

zip -j /tmp/naturalEarth.country.zip ${BASE}/country/naturalEarth.wbmapping.csv &&
zip -j /tmp/naturalEarth.state.zip ${BASE}/state/naturalEarth.wbmapping.csv &&
scp -Cp /tmp/naturalEarth.*.zip shilad.com:/var/www/html/www.shilad.com/wikibrain


