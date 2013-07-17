#!/bin/bash
# This bash script contains common shell functions and is included by all bash scripts
#

function die() {
    echo $1 >&2
    exit 1
}

export WP_BASE=..
export WP_CORE=$WP_BASE/wikapidia-core
export WP_LOADER=$WP_BASE/wikapidia-loader
export WP_MATRIX=$WP_BASE/wikAPIdia-matrix
export WP_MAPPER=$WP_BASE/wikAPIdia-mapper
export WP_PARENT=$WP_BASE/wikAPIdia-parent
export WP_PARSER=$WP_BASE/wikapidia-parser
export WP_UTILS=$WP_BASE/wikapidia-utils
export WP_DOWNLOAD=$WP_BASE/wikAPIdia-download


[ -d ${WP_BASE} ] || die "missing base directory ${WP_BASE}"

for d in "${WP_CORE}" "${WP_LOADER}" "${WP_MAPPER}" "${WP_PARENT}" "${WP_PARSER}" "${WP_UTILS}" "${WP_MATRIX}" "${WP_DOWNLOAD}" ; do
    [ -d "$d" ] || die "missing module directory $d"
done
                                                     
# source all util scripts here, so that other scripts only
# need to source this script to source everything
source ${WP_UTILS}/src/main/scripts/conf.sh


function compile() {
    echo compiling... &&
    (cd ${WP_PARENT} && mvn compile ) ||
    die "compilation failed"

    echo finished compiling
}

function execClass() {
    class=$1
    shift

    localclasspathfile=./target/localclasspath.txt
    if ! [ -f $localclasspathfile ]; then
        die "missing local classpath file $localclasspathfile"
    fi
    read < $localclasspathfile REMOTE_CLASSPATH
    java -cp "${REMOTE_CLASSPATH}:${LOCAL_CLASSPATH}" $JAVA_OPTS $class $@ ||
    die "executing java -cp ${REMOTE_CLASSPATH}:${LOCAL_CLASSPATH} $JAVA_OPTS $class $@ failed"
}



