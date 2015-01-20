from sklearn.manifold import SpectralEmbedding
from sklearn.preprocessing import normalize
import scipy.sparse
import numpy as np

import pickle

def convert_to_coo(titles, input, output):
    n = max(titles.keys()) + 1
    data = []
    rows = []
    cols = []

    ids = []
    for (i, line) in enumerate(open(input), 1):
        if i not in titles:
            continue
        ids.append(i)
        if i % 1000 == 0:
            print 'reading line', i
        for token in line.split():
            j, sim = token.split(':')
            j = int(j)
            if j in titles:
                data.append(float(sim))
                rows.append(i)
                cols.append(j)

    M = scipy.sparse.coo_matrix((data, (rows, cols)), shape=(n, n))
    f = open(output, 'wb')
    pickle.dump((M, ids), f, pickle.HIGHEST_PROTOCOL)
    f.close()

if __name__ == '__main__':
    titles = {}
    dir = '/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/cosim/'

    keepers = list([l.strip() for l in open(dir + '/concepts.txt')])
    keepers = set(keepers[:10000])

    for line in open(dir + '/ids.txt'):
        tokens = line.split('\t')
        if tokens[1].strip() in keepers:
            titles[int(tokens[0])] = tokens[2].strip()

    print 'read %d ids' % len(titles)
    convert_to_coo(titles, dir + '/matrix.txt', dir + '/matrix.pickle')
    (M, ids) = pickle.load(open(dir + '/matrix.pickle', 'rb'))

    # double-centered version of M
    n = M.shape[0]

    import sklearn.manifold
    # print 'done 1'
    model = sklearn.manifold.SpectralEmbedding(n_components=100, affinity='precomputed')
    # space = model.fit_transform(M)
    # normalize(space, copy=False)
    # space = scipy.sparse.csr_matrix(space)
    # space.eliminate_zeros()
    # print 'done'
    # actual = scipy.sparse.csr_matrix(M)
    # for id1 in ids:
    #     predicted = space[id1].dot(space.T)
    #     print 'neighbors for', titles[id1]
    #     # print type(cosim[id1]), cosim[id1].shape
    #     aindexes = actual[id1].toarray().squeeze().argsort()[::-1]
    #     pindexes = predicted.toarray().squeeze().argsort()[::-1]
    #     # print indexes.shape, type(indexes)
    #     for i in range(10):
    #         ta = titles.get(aindexes[i], 'Unknown')
    #         tp = titles.get(pindexes[i], 'Unknown')
    #         print '\t%d     %30s       %30s' % (i, ta, tp)