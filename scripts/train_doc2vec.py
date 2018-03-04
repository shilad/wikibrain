#!/usr/bin/env python3 -O
#
# Example script to train gensim's doc2vec model on a wikified corpus.
#
# Words are treated as plain-text tokens and title tokens are "t:page_id:page_title"
#

from collections import defaultdict
from gensim.models.doc2vec import TaggedDocument, Doc2Vec

import logging
import os
import random
import sys



def flatten(l):
    return  [item for sublist in l for item in sublist]

def read_word_freqs(path, min_freq):
    with open(path, encoding='utf-8') as f:
        freqs = defaultdict(int)
        for i, line in enumerate(f):
            if i % 1000000 == 0:
                logging.info('reading line %d of %s', i, path)
            (tag, count, word) = line.strip().split(' ')
            if tag == 'w' and int(count) >= min_freq:
                freqs[word.lower()] += int(count)
        return freqs


def line_iterator(path, kept_words):
    with open(path, encoding='utf-8') as f:
        article_line = 0        # line within article
        article_label = None    # label token for article

        for i, line in enumerate(f):
            if i % 1000000 == 0:
                logging.info('reading line %d of %s', i, path)
            line = line.strip()
            if not line or line.startswith('@WikiBrainCorpus'):
                pass
            elif line.startswith('@WikiBrainDoc'):
                (marker, page_id, title) = line.split('\t')
                article_label = 't:' + page_id + ':' + title.replace(' ', '_')
                article_line = 0
            else:
                tokens = flatten(translate_token(t, kept_words) for t in line.split())
                labels = []
                if article_label and article_line <= 4:
                    labels = [article_label]
                yield TaggedDocument(words=tokens, tags=labels)
                article_line += 1


CACHE = {}
def translate_token(token, kept_words):
    # Tries to match tokens like "Swarnapali:/w/en/53955546/Swarnapali"
    i = token.find(':/w/')
    if i > 0:
        w = sys.intern(token[:i])
        t = token[i+4:]
        if t not in CACHE and t.count('/') >= 2:
            (lang, page_id, title) = t.split('/', 2)
            CACHE[t] = 't:' + page_id + ':' + title
        ct = CACHE[t]
        if ct and bool(random.getrandbits(1)):
            return [w, ct]
        elif ct:
            return [ct, w]
        elif w.lower() in kept_words:
            return [sys.intern(w.lower())]
        else:
            return []
    elif token.lower() in kept_words:
        return [sys.intern(token.lower())]
    else:
        return []


def train(sentences):
    alpha = 0.05
    min_alpha = 0.001
    iters = 5
    model = Doc2Vec(
        dm=0,
        size=200,
        dbow_words=1,
        min_count=10,
        window=8,
        iter=iters,
        sample=1e-4,
        hs=0,
        negative=20,
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

if __name__ == '__main__':
    if len(sys.argv) not in (4, 5):
        sys.stderr.write('usage: %s corpus_dir min_freq model_output_path [save_as_binary]\n' % sys.argv[0])
        sys.exit(1)

    binary = False
    if len(sys.argv) == 5:
        binary = str2bool(sys.argv[4])

    logging.basicConfig(level=logging.INFO)
    (corpus_dir, min_freq, output_path) = sys.argv[1:4]
    min_freq = int(min_freq)

    freqs = read_word_freqs(corpus_dir + '/dictionary.txt', min_freq)
    logging.info('found %d words with min freq %d', len(freqs), min_freq)

    it = line_iterator(corpus_dir + '/corpus.txt', freqs.keys())
    sentences = list(it)
    model = train(sentences)
    model.save_word2vec_format(output_path, binary=binary)

