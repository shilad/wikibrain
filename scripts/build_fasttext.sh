#!/bin/bash
#
# Rebuilds word2vec models for a particular language
# usage: build-fasttext.sh [model-file-name] [lang1,lang2,..]
#        build-fasttext.sh [model-file-name]
#        build-fasttext.sh
#

set -e
set -x

langs=$@
if [ $# -eq 0 ]; then
        name=vectors.w2v.bin
        langs="en,de,simple,ar,az,bg,ca,cs,da,eo,es,et,eu,fa,fi,fr,gl,he,hi,hr,hu,id,it,ja,kk,ko,lt,ms,nl,nn,no,pl,pt,ro,ru,sk,sl,sr,sv,tr,uk,vi,vo,war,zh"
elif [ $# -eq 1 ]; then
        name=$1
        langs="en,de,simple,ar,az,bg,ca,cs,da,eo,es,et,eu,fa,fi,fr,gl,he,hi,hr,hu,id,it,ja,kk,ko,lt,ms,nl,nn,no,pl,pt,ro,ru,sk,sl,sr,sv,tr,uk,vi,vo,war,zh"
else
        name=$1
        langs="$2"
fi

echo building word2vec models for languages $langs

function do_lang() {
    lang=$1
    mkdir -p ./w2v/$lang
    aws s3 cp s3://wikibrain/w2v2/$lang/corpus.txt.bz2 ./w2v/$lang/
    aws s3 cp s3://wikibrain/w2v2/$lang/dictionary.txt.bz2 ./w2v/$lang/
    python36 ./scripts/train_fasttext.py ./w2v/$lang/ 300 20 ./w2v/$lang/$name
    aws s3 cp ./w2v/$lang/$name s3://wikibrain/w2v2/$lang/
    rm -rf ./w2v/$lang/
}

export -f do_lang

echo $langs | tr ',' '\n' | parallel -j 6 --line-buffer do_lang '{}'
