from collections import defaultdict
import logging
import os
import random
import sys

import smart_open
from gensim.models.doc2vec import TaggedDocument


class WikiBrainCorpus(object):
    """
    Reads in a WikiBrain-formatted corpus.
    Outputs a corpus of gensim TaggedDocuments
    Mentions of articles appear as t:page_id:Page_Title
    """
    def __init__(self, path, lower=False, min_freq=20):
        self.path = path
        self.freqs = self.read_word_freqs(os.path.join(path, 'dictionary.txt'), min_freq)
        logging.info('found %d words with min freq %d', len(self.freqs), min_freq)
        self.lower = lower
        self.mention_cache = {}

    def get_sentences(self):
        path = os.path.join(self.path, 'corpus.txt')
        with open_path_or_bz2(path) as f:
            article_line = 0        # line within article
            article_label = None    # label token for article

            for i, line in enumerate(f):
                if i % 1000000 == 0:
                    logging.info('reading line %d of %s', i, path)
                line = line.strip()
                if not line or starts_with_one_of(line, ['@WikiBrainCorpus', 'References ', 'ref ', 'thumb ']):
                    pass
                elif line.startswith('@WikiBrainDoc'):
                    (marker, page_id, title) = line.split('\t')
                    article_label = 't:' + page_id + ':' + title.replace(' ', '_')
                    article_line = 0
                else:
                    tokens = flatten(self.translate_token(t) for t in line.split())
                    labels = []
                    if article_label and article_line <= 4:
                        labels = [article_label]
                    yield TaggedDocument(words=tokens, tags=labels)
                    article_line += 1

    def clean(self, word):
        if self.lower:
            return word.lower()
        else:
            return word

    def translate_token(self, token):
        # Tries to match tokens like "Swarnapali:/w/en/53955546/Swarnapali"
        i = token.find(':/w/')
        if i > 0:
            w = sys.intern(token[:i])
            cw = self.clean(w)
            t = token[i+4:]
            if t not in self.mention_cache and t.count('/') >= 2:
                (lang, page_id, title) = t.split('/', 2)
                self.mention_cache [t] = 't:' + page_id + ':' + title
            ct = self.mention_cache[t]
            if ct and bool(random.getrandbits(1)):
                return [w, ct]
            elif ct:
                return [ct, w]
            elif cw in self.freqs:
                return [sys.intern(cw)]
            else:
                return []
        else:
            cw = self.clean(token)
            if cw in self.freqs:
                return [sys.intern(cw)]
            else:
                return []

    def read_word_freqs(self, path, min_freq):
        with open_path_or_bz2(path) as f:
            freqs = defaultdict(int)
            for i, line in enumerate(f):
                if i % 1000000 == 0:
                    logging.info('reading line %d of %s', i, path)
                (tag, count, word) = line.strip().split(' ')
                if tag == 'w' and int(count) >= min_freq:
                    freqs[word.lower()] += int(count)
            return freqs


def open_path_or_bz2(path):
    for p in (path, path + '.bz2'):
        if os.path.isfile(p):
            return smart_open.smart_open(p, 'r', encoding='utf-8')

def flatten(l):
    return  [item for sublist in l for item in sublist]

def starts_with_one_of(s, tokens):
    for t in tokens:
        if s.startswith(t):
            return True
    return False
