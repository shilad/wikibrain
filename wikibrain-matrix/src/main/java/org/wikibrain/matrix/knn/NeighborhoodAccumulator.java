package org.wikibrain.matrix.knn;

/*
 * A min-heap that tracks the n closest neighbors.
 * Each element in the neighborhood has a score and an id.
 */
public class NeighborhoodAccumulator {
    private double[] similarities;
    private int[] keys;
    private int size;

    /**
     * Create a neighborhood accumulator that holds at most n elements.
     * @param n
     */
    public NeighborhoodAccumulator(int n) {
        similarities = new double[n+1];
        keys = new int[n+1];
        size = 0 ;
        keys[0] = Integer.MIN_VALUE;
        similarities[0] = Double.NEGATIVE_INFINITY;
    }

    /**
     * Possibly add a neighbor to the neighborhood.
     * @param key
     * @param sim Similarity of the neighbor.
     */
    public final void visit(int key, double sim) {
        if (size < similarities.length - 1) {
            insert(key, sim);
        } else if (sim > similarities[1]) {
            assert(size == similarities.length - 1);
            removeMin();
            insert(key, sim);
        }
    }

    public Neighborhood get() {
        int  ids[] = new int[size];
        double scores[] = new double[size];
        for (int i = 1; i <= size; i++) {
            ids[i - 1] = keys[i];
            scores[i - 1] =similarities[i];
        }
        quickSort(ids, scores, 0, ids.length - 1);
        return new Neighborhood(ids, scores);
    }

    private int leftChild(int pos) {
        return 2*pos;
    }
    private int rightChild(int pos) {
        return 2*pos + 1;
    }

    private int parent(int pos) {
        return  pos / 2;
    }

    private boolean isLeaf(int pos) {
        return ((pos > size/2) && (pos <= size));
    }

    private void swap(int pos1, int pos2) {
        double tmpVal;

        tmpVal = similarities[pos1];
        similarities[pos1] = similarities[pos2];
        similarities[pos2] = tmpVal;

        int tmpKey;
        tmpKey = keys[pos1];
        keys[pos1] = keys[pos2];
        keys[pos2] = tmpKey;

    }

    private void insert(int key, double value) {
        assert(size < similarities.length - 1);
        size++;
        keys[size] = key;
        similarities[size] = value;
        int current = size;

        while (similarities[current] < similarities[parent(current)]) {
            swap(current, parent(current));
            current = parent(current);
        }
    }

    private int minKey() {
        return keys[1];
    }

    private double minValue() {
        return similarities[1];
    }

    private void removeMin() {
        swap(1,size);
        size--;
        if (size != 0)
            pushDown(1);
    }

    private void pushDown(int position) {
        int smallestChild;
        while (!isLeaf(position)) {
            smallestChild = leftChild(position);
            if ((smallestChild < size) && (similarities[smallestChild] > similarities[smallestChild+1]))
                smallestChild = smallestChild + 1;
            if (similarities[position] <= similarities[smallestChild]) return;
            swap(position,smallestChild);
            position = smallestChild;
        }
    }

    // Adapted from http://www.programcreek.com/2012/11/quicksort-array-in-java/
    private void quickSort(int colIds[], double colVals[], int low, int high) {
        if (colIds.length == 0 || low >= high)
            return;

        // pick the pivot
        int middle = (low + high) / 2;
        double pivot = colVals[middle];

        // partition around the pivot
        int i = low, j = high;
        while (i <= j) {
            while (colVals[i] > pivot) {
                i++;
            }
            while (colVals[j] < pivot) {
                j--;
            }
            if (i <= j) {
                int temp = colIds[i];
                double tempV = colVals[i];
                colIds[i] = colIds[j];
                colVals[i] = colVals[j];
                colIds[j] = temp;
                colVals[j] = tempV;
                i++;
                j--;
            }
        }

        //recursively sort two sub parts
        quickSort(colIds, colVals, low, j);
        quickSort(colIds, colVals, i, high);
    }
}
