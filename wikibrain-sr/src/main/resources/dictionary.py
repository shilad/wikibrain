import multiprocessing
import numpy as np
import os
import sys

from sklearn.decomposition import MiniBatchDictionaryLearning


if len(sys.argv) != 4:
    sys.stderr.write('usage: %s data_dir cosim_size dict_size\n' % sys.argv[0])
    sys.exit(1)

data_dir = sys.argv[1]
cosim_size = int(sys.argv[2])
dict_size = int(sys.argv[3])

M = np.memmap(data_dir + '/cosimilarity.dat', dtype='float32', mode='r', shape=(cosim_size, cosim_size))
M = M[:500]

d = MiniBatchDictionaryLearning(dict_size, n_iter=10, batch_size=1000, verbose=True, n_jobs=-1)

d.fit(M)

np.save(data_dir + '/components.bin', d.components_)

component_set = set()
for c in d.components_:
    component_set.add(tuple(c))

ids = open(data_dir + '/dictionary_indexes.txt', 'w')
for (i, row) in enumerate(M):
    if i % 1000:
        print('checking to see if row %d is a dictionary element' % i)
    if tuple(row) in component_set:
        ids.write(str(i) + '\n')
        component_set.remove(tuple(row))

ids.close()



