package org.wikibrain.spatial.maxima;

import org.apache.commons.io.FileUtils;
import org.wikibrain.spatial.cookbook.tflevaluate.MatrixGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class PickValidationPairs {
    public static void main(String args[]) throws IOException {
        MatrixGenerator.MatrixWithHeader matrix = MatrixGenerator.loadMatrixFile("srmatrix_en");
        Map<Integer, String> titles = new HashMap<Integer, String>();
        int rank = 0;
        for (String line : FileUtils.readLines(new File("PageHitListFullEnglish.txt"))) {
            if (rank++ > 100) {
                break;
            }
            String [] tokens = line.trim().split("\t");
            titles.put(Integer.valueOf(tokens[0]), tokens[1]);
        }

        List<SrPair> srs = new ArrayList<SrPair>();
        for (int id1 : titles.keySet()) {
            for (int id2 : titles.keySet()) {
                if (id1 < id2) {
                    if (!matrix.idToIndex.containsKey(id1) || !matrix.idToIndex.containsKey(id2)) {
                        continue;
                    }
                    int i = matrix.idToIndex.get(id1);
                    int j = matrix.idToIndex.get(id2);
                    double sr = matrix.matrix[i][j];
                    srs.add(new SrPair(id1, id2, titles.get(id1), titles.get(id2), sr));
                }
            }
        }

        Collections.sort(srs);
        Collections.reverse(srs);

        System.out.println("most related pairs:");
        for (int i = 0; i < 50; i++) {
            System.out.print("" + (i+1) + ". " + srs.get(i) + "\n");
        }

        Collections.reverse(srs);
        System.out.println("\n\nleast related pairs:");
        for (int i = 0; i < 50; i++) {
            System.out.print("" + (i+1) + ". " + srs.get(i) + "\n");
        }
    }

    public static class SrPair implements Comparable<SrPair> {
        int id1;
        int id2;
        String title1;
        String title2;
        Double sr;

        public SrPair(int id1, int id2, String title1, String title2, Double sr) {
            this.id1 = id1;
            this.id2 = id2;
            this.title1 = title1;
            this.title2 = title2;
            this.sr = sr;
        }

        @Override
        public int compareTo(SrPair that) {
            return this.sr.compareTo(that.sr);
        }

        public String toString() {
            return title1 + " vs " + title2 + ": " + sr;
        }
    }
}
