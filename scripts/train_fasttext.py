#!/usr/bin/env python3 -O
#
# Example script to train fasttext model on a wikified corpus.
#
# Words are treated as plain-text tokens and title tokens are "t:page_id:page_title"
#

from collections import defaultdict

from gensim.models.doc2vec import TaggedDocument, Doc2Vec
from urllib.request import urlretrieve

import logging
import math
import os
import os.path
import random
import subprocess
import sys
import zipfile


# As defined in https://arxiv.org/pdf/1607.05368.pdf
# We use these hyper-parameter values for WIKI (APNEWS): vector size = 300 (300),
# window size = 15 (15), min count = 20 (10), sub-sampling threshold = 10−5 (10−5 ),
# negative sample = 5, epoch = 20 (30). After removing low frequency words, the
# vocabulary size is approximately 670K for WIKI and 300K for AP-NEW.
#
from scripts.wikibrain_corpus import WikiBrainCorpus


def train(sentences):
    alpha = 0.025
    min_alpha = 0.0001
    iters = 20
    model = Doc2Vec(
        dm=0,
        size=300,
        min_count=20,
        dbow_words=1,
        window=15,
        iter=iters,
        sample=1e-5,
        hs=0,
        negative=10,
        alpha=alpha, min_alpha=alpha,
        workers=min(8, os.cpu_count())
    )
    model.build_vocab(sentences)
    for epoch in range(iters):
        logging.warn("BEGINNING ITERATION %d", epoch)
        random.shuffle(sentences)
        model.train(sentences, total_examples=len(sentences), epochs=1)

        # update alpha
        model.alpha -= (alpha - min_alpha) / iters
        model.alpha = max(model.alpha, min_alpha)
        model.min_alpha = model.alpha
    model.delete_temporary_training_data()
    return model

def str2bool(v):
    return v.lower() in ("yes", "true", "t", "1")


def write_fasttext(corpus, path_out):
    vocab = set()
    with open(path_out, 'w', encoding='utf-8') as out:
        for sentence in corpus.get_sentences():
            words = sentence.words
            if words:
                tags = sentence.tags
                repeats_per_tag = int(math.ceil(len(words) / 7))
                for t in tags:
                    for i in range(repeats_per_tag):
                        words.insert(random.randint(0, len(words)), t)
                out.write(' '.join(words))
                out.write('\n')
                vocab.update(words)
                vocab.update(tags)
    return len(vocab)

def train_fasttext(path_corpus, dims, path_vecs, vocab_size):
    if os.path.isdir('fastText-master/'):
        logging.info('fasttext already downloaded')
    else:
        url = 'https://github.com/facebookresearch/fastText/archive/master.zip'
        logging.info('downloading fast text from %s', url)
        urlretrieve(url, 'fasttext.zip')
        zip_ref = zipfile.ZipFile('fasttext.zip', 'r')
        zip_ref.extractall('./')
        zip_ref.close()

    if os.path.isfile('fastText-master/fasttext'):
        logging.info('fasttext already built')
    else:
        logging.info('building fasttext binary')
        subprocess.run(['make'], cwd='fastText-master', check=True)

    bucket_size = 2000000
    if vocab_size > 500000:
        bucket_size = 10000000

    # Parameters based on https://arxiv.org/pdf/1802.06893.pdf
    subprocess.run(['./fastText-master/fasttext',
                    'cbow', '-neg', '10', '-minCount', '10',
                    '-dim', str(dims), '-bucket', str(bucket_size),
                    '-input', path_corpus, '-output', path_vecs
                    ],
                   check=True)

    os.unlink(path_vecs + '.bin')
    os.rename(path_vecs + '.vec', path_vecs)


if __name__ == '__main__':
    if len(sys.argv) != 5:
        sys.stderr.write('usage: %s corpus_dir dims min_freq model_output_path\n' % sys.argv[0])
        sys.exit(1)

    (corpus_dir, dims, min_freq, output_path) = sys.argv[1:5]
    logging.basicConfig(format='%(asctime)s ' + corpus_dir + ': %(message)s', level=logging.INFO)
    dims = int(dims)
    min_freq = int(min_freq)

    corpus = WikiBrainCorpus(corpus_dir, min_freq=min_freq, lower=False)
    vocab_size = write_fasttext(corpus, corpus_dir + '/fasttext_corpus.txt')
    train_fasttext(corpus_dir + '/fasttext_corpus.txt', dims, output_path, vocab_size)

