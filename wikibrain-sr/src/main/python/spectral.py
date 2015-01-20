import numpy as np
import random
import pickle

import scipy.sparse

from scipy.spatial import distance

from sklearn.preprocessing import scale, normalize

def main(ids, M):
    D = np.diag(M.sum(axis=0))
    L = D - M
    L = scipy.sparse.csr_matrix(L)




if __name__ == '__main__':
    titles = {}
    dir = '/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/cosim/'

    keepers = list([l.strip() for l in open(dir + '/concepts.txt')])
    keepers = set(keepers[:5000])

    for line in open(dir + '/ids.txt'):
        tokens = line.split('\t')
        if tokens[1].strip() in keepers:
            titles[int(tokens[0])] = tokens[2].strip()

    print 'read %d ids' % len(titles)
    (M, ids) = pickle.load(open(dir + '/matrix.pickle', 'rb'))
    M = 0.5 * M + 0.5 * M.T
    M = M.tocsr()

    main(ids, M)
