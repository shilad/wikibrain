package org.wikibrain.sr.factorized;

import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SimUtils;
import org.wikibrain.utils.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class Tester {

    public static final int NUM_NEIGHBORS = 100;

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Env env = EnvBuilder.envFromArgs(args);

        Configurator c = env.getConfigurator();
        LocalPageDao pageDao = c.get(LocalPageDao.class);
        List<String> queries = Arrays.asList(
                "John Coltrane",
                "Squash (plant)",
                "Squash (sport)"
        );
        SRMetric metric1 = c.get(SRMetric.class, "ensemble", "language", "simple");
        SRMetric metric2 = c.get(SRMetric.class, "mostsimilarconcepts", "language", "simple");
        for (String q : queries) {
            LocalPage p = pageDao.getByTitle(Language.SIMPLE, q);
            System.out.println("Results for " + q + " are:");
            for (SRMetric m : Arrays.asList(metric1, metric2)) {
                SRResultList neighbors = m.mostSimilar(p.getLocalId(), 20);
                for (int i = 0; i < 10; i++) {
                    int id = neighbors.getId(i);
                    System.out.println("\t" + m.getName() + ": " + i + ". " + pageDao.getById(Language.SIMPLE, id));
                }
            }
        }
        File dir = new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/");
        File input = FileUtils.getFile(dir, "mostSimilar.matrix");
        File symInput = FileUtils.getFile(dir, "symMostSimilar.matrix");
        SparseMatrix m = new SparseMatrix(input);
        if (m.getRowIds().length != new TIntHashSet(m.getRowIds()).size()) {
            throw new IllegalArgumentException();
        }
        Matrix<? extends MatrixRow> sm = FactorizerUtils.makeSymmetric(new SparseMatrix(input));
        if (sm.getRowIds().length != new TIntHashSet(sm.getRowIds()).size()) {
            throw new IllegalArgumentException();
        }
        SparseMatrixWriter.write(sm, symInput);
//
//        for (String q : queries) {
//            LocalPage p = pageDao.getByTitle(Language.SIMPLE, q);
//            int neighbors[] = getNeighborsFromCosim(sm, p.getLocalId());
//            System.out.println("Results for " + q + " are:");
//            for (int i = 0; i < NUM_NEIGHBORS; i++) {
//                int id = neighbors[i];
//                System.out.println("\t" + i + ". " + pageDao.getById(Language.SIMPLE, id));
//            }
//        }

        DenseMatrix dm = new DenseMatrix(
                new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/factorized.matrix"));

        for (String q : queries) {
            LocalPage p = pageDao.getByTitle(Language.SIMPLE, q);
            int neighbors[] = getNeighbors(dm, p.getLocalId());
            System.out.println("Results for " + q + " are:");
            for (int i = 0; i < NUM_NEIGHBORS; i++) {
                int id = neighbors[i];
                System.out.println("\t" + i + ". " + pageDao.getById(Language.SIMPLE, id));
            }
        }
    }

    public static int[] getNeighbors(DenseMatrix m, int id) throws IOException {
        DenseMatrixRow row1 = m.getRow(id);
        if (row1 == null) {
            return null;
        }
        Scoreboard<Integer> neighbors = new Scoreboard<Integer>(NUM_NEIGHBORS);
        for (DenseMatrixRow row2 : m) {
            if (row2.getRowIndex() == id) {
                continue;
            }
            double sim = denseCosineSimilarity(row1, row2);
            neighbors.add(row2.getRowIndex(), sim);
        }
        int result[] = new int[NUM_NEIGHBORS];
        for (int i = 0; i < NUM_NEIGHBORS; i++) {
            result[i] = neighbors.getElement(i);
        }
        return result;
    }

    public static int[] getNeighborsFromCosim(Matrix m, int id) throws IOException {
        MatrixRow mr = m.getRow(id);
        if (mr == null) return null;
        Scoreboard<Integer> b = new Scoreboard<Integer>(NUM_NEIGHBORS);
        for (int i = 0; i < mr.getNumCols(); i++) {
            b.add(mr.getColIndex(i), mr.getColValue(i));
        }
        int [] neighbors = new int[b.size()];
        for (int i = 0; i < b.size(); i++) {
            neighbors[i] = b.getElement(i);
        }
        return neighbors;
    }

    public static int[] getNeighbors(Matrix<MatrixRow> m, int id) throws IOException {
        MatrixRow row1 = m.getRow(id);
        if (row1 == null) {
            return null;
        }
        Scoreboard<Integer> neighbors = new Scoreboard<Integer>(NUM_NEIGHBORS);
        for (MatrixRow row2  : m) {
            if (row2.getRowIndex() == id) {
                continue;
            }
            double sim = SimUtils.cosineSimilarity(row1, row2);
            neighbors.add(row2.getRowIndex(), sim);
        }
        int result[] = new int[NUM_NEIGHBORS];
        for (int i = 0; i < NUM_NEIGHBORS; i++) {
            result[i] = neighbors.getElement(i);
        }
        return result;
    }


    public static double denseCosineSimilarity(DenseMatrixRow x, DenseMatrixRow y) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;
        for (int i = 0; i < x.getNumCols(); i++) {
            double xv = x.getColValue(i);
            double yv = y.getColValue(i);
            xDotX += xv * xv;
            yDotY += yv * yv;
            xDotY += xv * yv;
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }
}
