#!/bin/bash
#
# 1. Imports a language edition of Wikipedia
# 2. Exports a wikified corpus
# 4. Uploads the corpus to s3
#

if [ $# -ne 1 ]; then 
	echo "usage: $0 wp_lang_code"
	exit 1
fi


mem_gb=$(free -h | gawk '/Mem:/{print $2}' | rev | cut -c 2- | rev)
wp_lang=$1
wp_db=wikibrain_${wp_lang}
base_dir=`pwd`/base_${wp_lang}
java_memory=$((mem_gb * 2 / 5))000M
num_hyperthreads=$(grep -c ^processor /proc/cpuinfo)
max_threads=$((num_hyperthreads / 2))
max_threads=$((max_threads>12?12:max_threads))

export PGPASSWORD=wikibrain
export JAVA_OPTS="-d64 -Xmx${java_memory} -server"

set -e
set -x

# Set up the environment
#
psql -h 127.0.0.1 -U wikibrain -c "CREATE DATABASE $wp_db" postgres || 
	echo "couldn't create database $wp_db. it probably exists, so we will continue" >&2

[ -d $base_dir ] || mkdir -p $base_dir

cat <<EOF > $base_dir/lang.conf
baseDir : "${base_dir}"
dao.dataSource.default : psql
dao.dataSource.psql {
    username : wikibrain
    password : wikibrain
    connectionsPerPartition : 5
    url : "jdbc:postgresql://localhost/${wp_db}"
}
EOF

mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller

# Import language for wikipedia
./wb-java.sh org.wikibrain.Loader -l ${wp_lang} -c ${base_dir}/lang.conf -h ${max_threads}

# Build the necessary SR model
./wb-java.sh org.wikibrain.sr.SRBuilder -m prebuiltword2vec -c ${base_dir}/lang.conf -f -h ${max_threads}

# Create the word2vec corpus
./wb-java.sh org.wikibrain.sr.wikify.CorpusCreatorMain -p wikified -o ${base_dir}/corpus_w2v --desiredPrecision 0.98 -c ${base_dir}/lang.conf -h ${max_threads}

# Upload to s3
pbzip2 -v ${base_dir}/corpus_w2v/dictionary.txt
pbzip2 -v ${base_dir}/corpus_w2v/corpus.txt

aws s3 cp ${base_dir}/corpus_w2v/dictionary.txt.bz2 s3://wikibrain/w2v2/${wp_lang}/
aws s3 cp ${base_dir}/corpus_w2v/corpus.txt.bz2 s3://wikibrain/w2v2/${wp_lang}/
