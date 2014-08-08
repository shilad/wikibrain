package org.wikibrain.sr.factorization;

import com.google.common.collect.Sets;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.MatrixRow;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.sr.utils.KnownSim;
import org.wikibrain.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public class FactorizationEvaluator {
    private final LocalPageDao pageDao;
    private final DatasetDao datasetDao;
    private final Set<String> datasetNames;
    private final Language language;

    public FactorizationEvaluator(Language language, LocalPageDao pageDao, DatasetDao datasetDao) {
        this(language, pageDao, datasetDao, Sets.newHashSet("wordsim353.txt", "radinsky.txt", "MTURK-771.csv"));
    }

    public FactorizationEvaluator(Language language, LocalPageDao pageDao, DatasetDao datasetDao, Set<String> datasetNames) {
        this.language = language;
        this.datasetNames = datasetNames;
        this.pageDao = pageDao;
        this.datasetDao = datasetDao;
    }

    public void evaluate(DenseMatrix articles, DenseMatrix topics) throws IOException, DaoException {
        evaluateSimilarity(articles);
    }

    public void evaluateReconstruction() {

    }

    public void evaluateSimilarity(DenseMatrix articles) throws DaoException, IOException {
        TDoubleList actual = new TDoubleArrayList();
        TDoubleList predicted = new TDoubleArrayList();
        for (String name : datasetNames) {
            Dataset ds = datasetDao.get(Language.SIMPLE, name);
            ds.normalize();
            for (KnownSim ks : ds.getData()) {
                MatrixRow row1 = articles.getRow(ks.wpId1);
                MatrixRow row2 = articles.getRow(ks.wpId2);
                if (row1 != null && row2 != null) {
                    actual.add(ks.similarity);
                    predicted.add(cosineSim(row1, row2));
                }
            }
        }
        System.out.println("Spearman's on " + actual.size() + " is " +
                new SpearmansCorrelation().correlation(actual.toArray(), predicted.toArray()));
    }

    private double cosineSim(MatrixRow row1, MatrixRow row2) {
        return SimUtils.cosineSimilarity(row1.asTroveMap(), row2.asTroveMap());
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        FactorizationEvaluator eval = new FactorizationEvaluator(
                env.getLanguages().getDefaultLanguage(),
                env.getConfigurator().get(LocalPageDao.class),
                env.getConfigurator().get(DatasetDao.class)
        );
        DenseMatrix articles = new DenseMatrix(new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/topics/simple/inlink-funk/row_estimates.matrix"));
        DenseMatrix topics = null;
        eval.evaluate(articles, null);
    }
}
