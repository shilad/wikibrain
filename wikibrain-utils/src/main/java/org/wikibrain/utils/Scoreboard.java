package org.wikibrain.utils;

/**
 * Priority queue that stores the largest or smallest n scores.
 *
 * @author Shilad Sen
 */
public class Scoreboard<T> {
    public static enum Order {
        INCREASING,
        DECREASING
    }

    private T[] elements;
    private double[] scores;

    private Order order;

    public Scoreboard(int maxCapacity) {
        this(maxCapacity, Order.DECREASING);
    }

    public Scoreboard(int maxCapacity, Order order) {
        this.order = order;
        this.elements = (T[]) new Object[maxCapacity];
        this.scores = new double[maxCapacity];
    }

    public void add(T element, double score) {
        if (!belongs(score)) {
            return;
        }

        // Find the correct index.
        int nextIndex = scores.length - 1;
        if (order == Order.DECREASING) {
            for (nextIndex = scores.length - 1; nextIndex >= 0; nextIndex--) {
                if (elements[nextIndex] != null && score < scores[nextIndex]) {
                    break;
                }
            }
        } else {
            for (nextIndex = scores.length - 1; nextIndex >= 0; nextIndex--) {
                if (elements[nextIndex] != null && score > scores[nextIndex]) {
                    break;
                }
            }
        }
        int targetIndex = nextIndex + 1;
        if (targetIndex >= scores.length) {
            throw new IllegalStateException();
        }

        // Slide things down, drop the last element
        for (int i = scores.length - 2 ;i >= targetIndex; i--) {
            scores[i+1] = scores[i];
            elements[i+1] = elements[i];
        }

        // insert the right element
        scores[targetIndex] = score;
        elements[targetIndex] = element;
    }

    public int size() {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null) {
                return i;
            }
        }
        return elements.length;
    }

    public double getScore(int i) {
        return scores[i];
    }

    public T getElement(int i) {
        return elements[i];
    }

    private boolean belongs(double score) {
        if (elements[elements.length - 1] == null) {
            return true;
        }
        double lastScore = scores[elements.length - 1];
        if (order == Order.DECREASING) {
            return (score > lastScore);
        } else {
            return (score < lastScore);
        }
    }
}
