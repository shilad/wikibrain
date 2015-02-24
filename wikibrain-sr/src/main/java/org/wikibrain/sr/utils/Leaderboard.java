package org.wikibrain.sr.utils;

import org.wikibrain.sr.SRResultList;

public class Leaderboard {
    private double[] values;
    private int[] keys;
    private int size;

    public Leaderboard(int n) {
        values = new double[n+1];
        keys = new int[n+1];
        size = 0 ;
        keys[0] = Integer.MIN_VALUE;
        values[0] = Double.NEGATIVE_INFINITY;
    }

    public void tallyScore(int key, double value) {
        if (size < values.length - 1) {
            insert(key, value);
        } else if (value > values[1]) {
            assert(size == values.length - 1);
            removeMin();
            insert(key, value);
        }
    }

    public SRResultList getTop() {
        SRResultList scores = new SRResultList(size);
        for (int i = 1; i <= size; i++) {
            scores.set(i - 1, keys[i], values[i]);
        }
        scores.sortDescending();
        return scores;
    }

    public void print() {
        int i;
        for (i=1; i<=size;i++)
            System.out.print(values[i] + " ");
        System.out.println();
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

        tmpVal = values[pos1];
        values[pos1] = values[pos2];
        values[pos2] = tmpVal;

        int tmpKey;
        tmpKey = keys[pos1];
        keys[pos1] = keys[pos2];
        keys[pos2] = tmpKey;

    }

    private void insert(int key, double value) {
        assert(size < values.length - 1);
        size++;
        keys[size] = key;
        values[size] = value;
        int current = size;

        while (values[current] < values[parent(current)]) {
            swap(current, parent(current));
            current = parent(current);
        }
    }

    private int minKey() {
        return keys[1];
    }

    private double minValue() {
        return values[1];
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
            if ((smallestChild < size) && (values[smallestChild] > values[smallestChild+1]))
                smallestChild = smallestChild + 1;
            if (values[position] <= values[smallestChild]) return;
            swap(position,smallestChild);
            position = smallestChild;
        }
    }
}
