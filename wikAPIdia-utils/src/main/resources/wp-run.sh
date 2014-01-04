#!/bin/bash
#
# This script is installed by running org.wikapidia.utils.ResourceInstaller
#
# You should not edit this file outside of the sr/main/resources source directory
# because it may be overwritten.
#
# TODO: Make this script work with maven
#

# Configuration parameters.
# These can be overriden by changing wp-conf.sh

# Maven pom
POM=pom.xml

# Java options
JAVA_OPTS="$JAVA_OPTS -server -ea"

# Directories to search for the wp-conf.sh file
CONF_SEARCH_DIRS="
`dirname $0`
.
..
"

# Destination of compiled jars and dependencies
WP_LIB="$(pwd)/lib"


# Specify classpath
WP_CLASSPATH="${WP_LIB}/*"
if [ -n "${CLASSPATH}" ]; then
    WP_CLASSPATH="${CLASSPATH}:${WP_CLASSPATH}"
fi

# Java executable
JAVA_BIN=java


# Displays an error message and exits
function die() {
    echo $1 >&2
    exit 1
}

if [ -z "$1" ]; then
    die "usage: $0 package.and.Class arg1 arg2 ...."
fi


# check to see if a configuration file exists and source it if so.
confFile=""
for dir in ${CONF_SEARCH_DIRS}; do
    echo "checking dir ${dir}"
    f="${dir}/wp-conf.sh"
    if [ -f ${f} ]; then
        confFile="${f}"
        break;
    fi
done
if [ -z "$confFile" ]; then
    echo "Configuration file wp-conf.sh doesn't exist in search path ${CONF_SEARCH_DIRS}. using default configuration." >&2
else
    echo "Sourcing configuration file ${confFile}" >&2
    source ${confFile}
fi


# Find the maven pom
if [ -f ${POM} ]; then
    true # keep existing value
elif [ -f "wikAPIdia-parent/pom.xml" ]; then
    POM=wikAPIdia-parent/pom.xml
else
    die "Maven pom.xml file ${POM} does not exist"
fi
echo "Using maven pom ${POM}" >&2


# Compile the project and build the classpath files
rm -rf "${WP_LIB}/*.jar"
mvn -f "${POM}" clean compile package install -DskipTests || die "compilation failed"
mvn -f "${POM}" dependency:copy-dependencies -DoutputDirectory="${WP_LIB}" || die "copying dependencies failed"

# Grab the latest compiled version of source jars
cp -p */target/*.jar target/*.jar "${WP_LIB}"


# Run the
echo "executing ${JAVA_BIN} -cp \"${WP_CLASSPATH}\" $JAVA_OPTS $class $@"

${JAVA_BIN} -cp "${WP_CLASSPATH}" $JAVA_OPTS $class $@