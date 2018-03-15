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

export PGPASSWORD=wikibrain

set -e
set -x

# Set up the environment
#
psql -h 127.0.0.1 -U wikibrain -c "DROP DATABASE $wp_db" postgres || 
	echo "couldn't create database $wp_db. it probably doesnt exist, so we will continue" >&2

[ -d $base_dir ] && rm -rf $base_dir
