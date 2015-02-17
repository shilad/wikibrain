package org.wikibrain.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestScoreboard {
    @Test
    public void testIncreasing() {
        Scoreboard<Integer> scoreboard = new Scoreboard<Integer>(10);
        List<Score> scores = new ArrayList<Score>();
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            Score score = new Score(i, random.nextDouble());
            scoreboard.add(score.element, score.score);
            scores.add(score);
        }
        Collections.sort(scores);
        Collections.reverse(scores);

        assertEquals(10, scoreboard.size());
        for (int i = 0; i < scoreboard.size(); i++) {
            assertEquals(scores.get(i).element, scoreboard.getElement(i));
            assertEquals(scores.get(i).score, scoreboard.getScore(i), 0.00001);
        }
    }

    @Test
    public void testDecreasing() {
        Scoreboard<Integer> scoreboard = new Scoreboard<Integer>(10, Scoreboard.Order.INCREASING);
        List<Score> scores = new ArrayList<Score>();
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            Score score = new Score(i, random.nextDouble());
            scoreboard.add(score.element, score.score);
            scores.add(score);
        }
        Collections.sort(scores);

        assertEquals(10, scoreboard.size());
        for (int i = 0; i < scoreboard.size(); i++) {
            assertEquals(scores.get(i).element, scoreboard.getElement(i));
            assertEquals(scores.get(i).score, scoreboard.getScore(i), 0.00001);
        }
    }

    @Test
    public void testShort() {
        Scoreboard<Integer> scoreboard = new Scoreboard<Integer>(100, Scoreboard.Order.INCREASING);
        List<Score> scores = new ArrayList<Score>();
        Random random = new Random();
        for (int i = 0; i < 33; i++) {
            Score score = new Score(i, random.nextDouble());
            scoreboard.add(score.element, score.score);
            scores.add(score);
        }
        Collections.sort(scores);

        assertEquals(33, scoreboard.size());
        for (int i = 0; i < scoreboard.size(); i++) {
            assertEquals(scores.get(i).element, scoreboard.getElement(i));
            assertEquals(scores.get(i).score, scoreboard.getScore(i), 0.00001);
        }
    }

    public static class Score implements Comparable<Score> {
        Integer element;
        double score;

        public Score(Integer element, double value) {
            this.element = element;
            this.score = value;
        }

        @Override
        public int compareTo(Score o) {
            if (score < o.score) {
                return -1;
            } else if (score > o.score) {
                return +1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Score{" +
                    "element=" + element +
                    ", score=" + score +
                    '}';
        }
    }
}
