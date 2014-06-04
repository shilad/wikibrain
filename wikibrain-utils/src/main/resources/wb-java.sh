#!/bin/bash
#
# This script is installed by running org.wikibrain.utils.ResourceInstaller
#
# You should not edit this file outside of the sr/main/resources source directory
# because it may be overwritten.
#
# TODO: Make this script work with maven
#

# utility: resolve a (possilble) relative path to a full path
# from http://stackoverflow.com/a/7126780/141245
#
full_path() {
    cd "$1" 2>/dev/null || return $?  # cd to desired directory; if fail, quell any error messages but return exit status
    echo "`pwd -P`" # output full, link-resolved path
}

# Configuration parameters.
# These can be overriden by setting your environment


# Base WikiBrain directory. Should be the parent project directory in multi-maven projects
WB_DIR=$(full_path "${WB_DIR:-.}")

# Java options
WB_JAVA_OPTS="${WB_JAVA_OPTS:-${JAVA_OPTS} -server -ea}"

# Directories to search for the wp-conf.sh file
WB_CONF_PATHS="
${WB_CONF}
`dirname $0`/wp-conf.sh
./wp-conf.sh
${WB_DIR}/wp-conf.sh
"

# Search path for maven pom
WB_POM_PATHS="
${WB_POM}
${WB_DIR}/pom.xml
${WB_DIR}/wikAPIdia-parent/pom.xml
"

# Destination of compiled jars and dependencies
if [ ! -e "${WB_LIB:-${WB_DIR}/lib}" ]; then
    mkdir "${WB_LIB:-${WB_DIR}/lib}"
fi
WB_LIB=$(full_path "${WB_LIB:-${WB_DIR}/lib}")

# Specify default classpath. This will be updated later, and CLASSPATH will be prepended to it.
WB_CLASSPATH="${WB_CLASSPATH:-${WB_LIB}/*}"

# Java executable
WB_JAVA_BIN="${WB_JAVA_BIN:-java}"

# Standard maven targets. Clean will be prepended to it if it is the first argument
WB_MVN_TARGETS="${WB_MVN_TARGETS:-compile install}"

echo -e "WikiBrain environment settings follow. WB_CLASSPATH is updated again later\n" >&2
(set -o posix ; set) | grep WB_ | sed -e 's/^/    /' >&2
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
    WB_MVN_TARGETS="clean ${WB_MVN_TARGETS}"
    shift
fi
if [ -z "$1" ]; then
    die "usage: $0 [clean] package.and.Class arg1 arg2 ...."
fi


# Find the maven pom
pom="$(select_first_file ${WB_POM_PATHS})"
if [ -z "${pom}" ]; then
    die "Maven pom.xml file not found in ${WB_POM_PATHS}"
fi
echo "Using maven pom ${pom}" >&2


# Compile the project and build the classpath files
rm -rf "${WB_LIB}/*.jar"
mvn -f "${pom}" -q -DskipTests ${WB_MVN_TARGETS} || die "compilation failed"
mvn -f "${pom}" -q dependency:copy-dependencies -DoutputDirectory="${WB_LIB}" || die "copying dependencies failed"

# Update classpath with latest version of jars, etc.
for srcdir in $(find "${WB_DIR}" -type d -print | grep 'target/classes$'); do
    WB_CLASSPATH="${srcdir}:${WB_CLASSPATH}"
done
if [ -n "${CLASSPATH}" ]; then
    WB_CLASSPATH="${CLASSPATH}:${WB_CLASSPATH}"
fi

echo "final WB_CLASSPATH is $WB_CLASSPATH" >&2



# Run the command
echo "executing \"${WB_JAVA_BIN}\" -cp \"${WB_CLASSPATH}\" $WB_JAVA_OPTS $@"

exec "${WB_JAVA_BIN}" -cp "${WB_CLASSPATH}" ${WB_JAVA_OPTS} $@
