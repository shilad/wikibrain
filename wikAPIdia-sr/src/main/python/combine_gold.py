#!/usr/bin/python -O

import collections
import os
import sys

MIN_SIM = 0
MAX_SIM = 1.0

def main(paths):
    scores = collections.defaultdict(list)
    for path in sys.argv[1:]:
        for (w1, w2, score) in getSims(path):
            scores[(w1, w2)].append(score)

    num_duplicate = 0
    for ((w1, w2), values) in scores.items():
        if len(values) > 1:
            #warn('pair (%s, %s) has scores: %s' % (w1, w2, values))
            num_duplicate += 1
        print '%s\t%s\t%s' % (w1, w2, mean(values))

def clean(s):
    return s.replace('_', ' ').lower()

def getSims(path):
    delim = None
    if path.endswith('csv'):
        delim = ','
    elif path.endswith('tab'):
        delim = '\t'
    else:
        raise Exception('unknown delimiter for file ' + path)

    name = os.path.basename(path).split('.')[0]
    sims = []
    for line in open(path):
        try:
            (w1, w2, s) = line.strip().split(delim)
            sims.append((w1, w2, float(s)))
        except:
            warn('invalid line in %s: %s' % (`path`, `line`))

    min_sim = min([s for w1, w2, s in sims])
    max_sim = max([s for w1, w2, s in sims])
    warn('%s has sims in range [%s to %s]' % (`path`, min_sim, max_sim))

    result = []
    for (w1, w2, s) in sims:
        if w1 > w2: t = w1; w1 = w2; w2 = t
        norm_s = (s - min_sim) / (max_sim - min_sim)
        new_s = norm_s * (MAX_SIM - MIN_SIM) + MIN_SIM
        result.append((clean(w1), clean(w2), new_s))

    return result

def mean(values):
    return 1.0 * sum(values) / len(values)

def warn(message):
    sys.stderr.write(message + '\n')

if __name__ == '__main__':
    main(sys.argv[1:])
