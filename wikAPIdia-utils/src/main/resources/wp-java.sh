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
# These can be overriden by setting your environment

# Base Wikapidia directory. Should be the parent project directory in multi-maven projects
WP_DIR="${WP_DIR:-.}"

# Java options
WP_JAVA_OPTS="${WP_JAVA_OPTS:-${JAVA_OPTS} -server -ea}"

# Directories to search for the wp-conf.sh file
WP_CONF_PATHS="
${WP_CONF}
`dirname $0`/wp-conf.sh
./wp-conf.sh
${WP_DIR}/wp-conf.sh
"

# Search path for maven pom
WP_POM_PATHS="
${WP_POM}
${WP_DIR}/pom.xml
${WP_DIR}/wikAPIdia-parent/pom.xml
"

# Destination of compiled jars and dependencies
WP_LIB="${WP_LIB:-${WP_DIR}/lib}"

# Specify default classpath. This will be updated later, and CLASSPATH will be prepended to it.
WP_CLASSPATH="${WP_CLASSPATH:-${WP_LIB}/*}"

# Java executable
WP_JAVA_BIN="${WP_JAVA_BIN:-java}"

# Standard maven targets. Clean will be prepended to it if it is the first argument
WP_MVN_TARGETS="${WP_MVN_TARGETS:-compile install}"

echo -e "Wikapidia environment settings follow. WP_CLASSPATH is updated again later\n" >&2
(set -o posix ; set) | grep WP_ | sed -e 's/^/    /' >&2
echo "" >&2

# Displays an error message and exits
function die() {
    echo $1 >&2
    exit 1
}

# Select the first existing file among several choices
function select_first_file() {
    for file in $@; do
        if [ -f "${file}" ]; then
            echo "${file}"
			break
        fi
    done
}


# Process initial arguments and make sure they are valid.
if [ "$1" == "clean" ]; then
    WP_MVN_TARGETS="clean ${WP_MVN_TARGETS}"
    shift
fi
if [ -z "$1" ]; then
    die "usage: $0 [clean] package.and.Class arg1 arg2 ...."
fi


# Find the maven pom
pom="$(select_first_file ${WP_POM_PATHS})"
if [ -z "${pom}" ]; then
    die "Maven pom.xml file not found in ${WP_POM_PATHS}"
fi
echo "Using maven pom ${pom}" >&2


# Compile the project and build the classpath files
rm -rf "${WP_LIB}/*.jar"
mvn -f "${pom}" -q -DskipTests ${WP_MVN_TARGETS} || die "compilation failed"
mvn -f "${pom}" -q dependency:copy-dependencies -DoutputDirectory="${WP_LIB}" || die "copying dependencies failed"

# Update classpath with latest version of jars, etc.
for srcdir in $(find "${WP_DIR}" -type d -print | grep 'target/classes$'); do
    WP_CLASSPATH="${srcdir}:${WP_CLASSPATH}"
done
if [ -n "${CLASSPATH}" ]; then
    WP_CLASSPATH="${CLASSPATH}:${WP_CLASSPATH}"
fi



# Run the command
echo "executing \"${WP_JAVA_BIN}\" -cp \"${WP_CLASSPATH}\" $WP_JAVA_OPTS $@"

exec "${WP_JAVA_BIN}" -cp "${WP_CLASSPATH}" ${WP_JAVA_OPTS} $@
