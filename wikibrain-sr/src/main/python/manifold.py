from sklearn.manifold import SpectralEmbedding
import scipy.sparse

import pickle

def convert_to_coo(input, output):
    n = max(ids.keys()) + 1
    data = []
    rows = []
    cols = []
    for (i, line) in enumerate(open(input)):
        if i % 1000 == 0:
            print 'reading line', i
        for token in line.split():
            j, sim = token.split(':')
            j = int(j)
            data.append(float(sim))
            rows.append(i+1)
            cols.append(j)

    M = scipy.sparse.coo_matrix((data, (rows, cols)), shape=(n, n))
    f = open(output, 'wb')
    pickle.dump(M, f, pickle.HIGHEST_PROTOCOL)
    f.close()

if __name__ == '__main__':
    ids = {}
    dir = '/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/cosim/'

    for line in open(dir + '/ids.txt'):
        tokens = line.split('\t')
        ids[int(tokens[0])] = tokens[2].strip()
    print 'read %d ids' % len(ids)
    #convert_to_coo(dir + '/matrix.txt', dir + '/matrix.pickle')
    M = pickle.load(open(dir + '/matrix.pickle', 'rb'))
    import sklearn.manifold
    print 'done 1'
    sklearn.manifold.spectral_embedding(M)
    print 'done'