#!/bin/bash
#
# 1. Imports a language edition of Wikipedia
# 2. Exports a wikified corpus
# 3. Builds a word2vec model using gensim
# 4. Uploads the model to s3
#

if [ $# -ne 1 ]; then 
	echo "usage: $0 wp_lang_code"
	exit 1
fi

wp_lang=$1
wp_db=wikibrain_${wp_lang}
base_dir=`pwd`/base_${wp_lang}
java_memory=64000M

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
./wb-java.sh org.wikibrain.Loader -l ${wp_lang} -c ${base_dir}/lang.conf

# Build the necessary SR model
./wb-java.sh org.wikibrain.sr.SRBuilder -m prebuiltword2vec -c ${base_dir}/lang.conf -f

# Create the word2vec corpus
./wb-java.sh org.wikibrain.sr.wikify.CorpusCreatorMain -p wikified -o ${base_dir}/corpus_w2v --desiredPrecision 0.98 -c ${base_dir}/lang.conf

# Train the word2vec model
python3 ./scripts/train_doc2vec.py ${base_dir}/corpus_w2v 20 ${base_dir}/vectors.${wp_lang}.bin True

# Upload to s3
aws s3 cp ${base_dir}/vectors.${wp_lang}.bin s3://wikibrain/w2v2/
