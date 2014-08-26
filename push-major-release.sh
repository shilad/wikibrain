#!/bin/bash

ALL_JARS=`pwd`/.tmp/all-jars

version=$(grep wikibrain.version wikibrain-parent/pom.xml | head -1  | sed 's/.*<wikibrain.version>//' | sed 's/<\/wikibrain.version>//')
echo "version is $version"
target=`pwd`/wikibrain-withdeps-${version}.zip
shadedjar=`pwd`/wikibrain/wikibrain-withdeps-${version}.jar

rm -rf $ALL_JARS $target
mvn clean compile package deploy -P release-version -DskipTests=true &&
mvn dependency:copy-dependencies -DoutputDirectory=$ALL_JARS &&
cp -p wikibrain*/target/wikibrain*${version}.jar $ALL_JARS &&
(cd $ALL_JARS && zip $target *.jar) &&
scp -p $target $shadedjar shilad.com:/var/www/html/www.shilad.com/wikibrain

