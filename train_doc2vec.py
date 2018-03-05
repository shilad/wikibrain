#!/usr/bin/env python2.7 -O

from gensim.models.doc2vec import LabeledSentence, Doc2Vec

import logging
import os
import random
import re
import sys



def flatten(l):
    return  [item for sublist in l for item in sublist]

def read_word_freqs(path, min_freq):
    with open(path, encoding='utf-8') as f:
        freqs = {}
        for i, line in enumerate(f):
            if i % 1000000 == 0:
                logging.info('reading line %d of %s', i, path)
            (tag, count, word) = line.strip().split(' ')
            if tag == 'w' and int(count) >= min_freq:
                freqs[word] = int(count)
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
                tokens = flatten(translate_token(t, kept_words) for t in line.split(' '))
                labels = []
                if article_label and article_line <= 4:
                    labels = [article_label]
                yield LabeledSentence(words=tokens, tags=labels)
                article_line += 1


# Tries to match tokens like "Swarnapali:/w/en/53955546/Swarnapali"
#
MATCH_TITLE = re.compile(r"\/w\/[^ \/]+\/(\d+)\/([^ ]+)").match
CACHE = {}
def translate_token(token, kept_words):
    i = token.find(':/w/')
    if i > 0:
        w = sys.intern(token[:i])
        t = token[i+1:]
        if t not in CACHE:
            m = MATCH_TITLE(t)
            if m:
                CACHE[t] = 't:' + m.group(1) + ':' + m.group(2)
            else:
                CACHE[t] = None
        ct = CACHE[t]
        if ct and bool(random.getrandbits(1)):
            return [w, ct]
        elif ct:
            return [ct, w]
        elif w in kept_words:
            return [w]
        else:
            return []
    elif token in kept_words:
        return [sys.intern(token)]
    else:
        return []

def train(sentences):
    alpha = 0.05
    min_alpha = 0.001
    iters = 5
    model = Doc2Vec(
        size=200, 
        min_count=5, 
        window=10, 
        iter=iters, 
        sample=1e-4, 
        negative=40,
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
    
if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.stderr.write('usage: %s corpus_dir min_freq model_output_path\n' % sys.argv[0])
        sys.exit(1)

    logging.basicConfig(level=logging.INFO)
    (corpus_dir, min_freq, output_path) = sys.argv[1:]
    min_freq = int(min_freq)

    freqs = read_word_freqs(corpus_dir + '/dictionary.txt', min_freq)
    logging.info('found %d words with min freq %d', len(freqs), min_freq)

    it = line_iterator(corpus_dir + '/corpus.txt', freqs.keys())
    sentences = list(it)
    model = train(sentences)
    model.save(output_path)
