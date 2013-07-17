#!/usr/bin/python

import collections
import sys
from math import floor
from random import sample

NBINS = int(sys.argv[1])

min_threshold = -10000000000
if len(sys.argv) >= 3:
    min_threshold = float(sys.argv[2])


data = []
for line in sys.stdin:
    (phrase1, phrase2, sim) = line.split('\t')
    sim = float(sim.strip())
    if sim >= min_threshold:
        data.append((phrase1.strip(), phrase2.strip(), sim, line))

min_sim = min([s for (p1, p2, s, l) in data])
max_sim = max([s for (p1, p2, s, l) in data])

bins = collections.defaultdict(list)
for (p1, p2, s, l) in data:
    i = int(floor((s - min_sim) / (.0001 + max_sim - min_sim) * NBINS))
    bins[i].append((p1, p2, s, l))

for i in xrange(NBINS):
    sys.stderr.write('bin %d has %d entries\n' % (i, len(bins[i])))
min_bin_size = min([len(b) for b in bins.values()])
sys.stderr.write('min bin size is %d\n' % min_bin_size)

for bin_data in bins.values():
    if len(bin_data) > min_bin_size:
        bin_data = sample(bin_data, min_bin_size)
    for (p1, p2, s, l) in bin_data:
        sys.stdout.write(l)
