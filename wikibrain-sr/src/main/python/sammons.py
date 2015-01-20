import numpy as np
import random
import pickle


from scipy.spatial import distance

from sklearn.preprocessing import scale, normalize

def main(ids, M):
    space = np.random.rand(max(ids)+1, 10) / 10
    #M = normalize(space)
    rate = 0.001
    for i in range(100):
        print 'doing', i
        iteration(rate, ids, M, space)
        rate *= 0.8

def iteration(rate, ids, M, space):
    ids1 = list(ids)
    random.shuffle(ids1)

    i = 0
    obj = 0
    for id1 in ids1:
        row = M[id1]
        indexes = list(range(len(row.indices)))
        random.shuffle(indexes)
        for i in indexes:
            id2 = row.indices[i]
            if id1 == id2:
                continue
            val = row.data[i]
            actual = max(1 - val, 0.01)

            # assert(isfinite(space[id1]))
            # assert(isfinite(space[id2]))
            # assert(isfinite(actual))
            predicted =  distance.euclidean(space[id1], space[id2])
            # assert(isfinite(predicted))
            delta = rate * (actual - predicted) / predicted
            # assert(isfinite(delta))

            corrections = delta * (space[id1] - space[id2])
            # print predicted, delta, corrections


            # assert(isfinite(corrections))
            space[id1] += corrections
            space[id2] -= corrections
            obj += abs((actual - predicted) / actual)
            i += 1
            # assert(isfinite(space[id1]))
            # assert(isfinite(space[id2]))

    print obj / i


def isfinite(X):
    return np.isfinite(X).all()


if __name__ == '__main__':
    titles = {}
    dir = '/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/cosim/'

    keepers = list([l.strip() for l in open(dir + '/concepts.txt')])
    keepers = set(keepers[:1000])

    for line in open(dir + '/ids.txt'):
        tokens = line.split('\t')
        if tokens[1].strip() in keepers:
            titles[int(tokens[0])] = tokens[2].strip()

    print 'read %d ids' % len(titles)
    (M, ids) = pickle.load(open(dir + '/matrix.pickle', 'rb'))
    M = 0.5 * M + 0.5 * M.T
    M = M.tocsr()

    main(ids, M)
