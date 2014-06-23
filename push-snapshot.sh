#!/bin/bash

ALL_JARS=`pwd`/.tmp/all-jars

version=$(grep version ./pom.xml | tail -1 | sed 's/.*<version>//' | sed 's/<\/version>//')
target=`pwd`/wikibrain-and-deps-${version}.zip

rm -rf $ALL_JARS $target
mvn clean compile package deploy -P release-snapshot -DskipTests=true &&
mvn dependency:copy-dependencies -DoutputDirectory=$ALL_JARS &&
cp -p wikibrain*/target/wikibrain*${version}.jar $ALL_JARS &&
(cd $ALL_JARS && zip $target *.jar) &&
scp -p $target shilad.com:/var/www/html/www.shilad.com/wikibrain


