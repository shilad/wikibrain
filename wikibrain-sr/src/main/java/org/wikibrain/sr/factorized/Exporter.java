package org.wikibrain.sr.factorized;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpCollectionUtils;
import sun.awt.SunHints;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class Exporter {
    private final Env env;
    private final Language lang;
    private final SRMetric metric;

    public Exporter(Env env) throws ConfigurationException {
        this.env = env;
        this.lang = env.getDefaultLanguage();
        this.metric = env.getConfigurator().get(SRMetric.class, "ensemble", "language", lang.getLangCode());
    }

    public void export(int numPages, File dest) throws DaoException, ConfigurationException, IOException {
        File tmpMatrix = File.createTempFile("cosim", "matrix");
        FileUtils.forceDeleteOnExit(tmpMatrix);
        FileUtils.deleteQuietly(tmpMatrix);
        final ValueConf vc = new ValueConf(-0.1f, 1.1f);
        final SparseMatrixWriter writer = new SparseMatrixWriter(tmpMatrix, vc);

        final TIntSet ids = chooseSample(numPages);
        final int [] idList = ids.toArray();
        ParallelForEach.range(0, idList.length, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                SRResultList neighbors = metric.mostSimilar(idList[i], ids.size() / 10, ids);
                SparseMatrixRow row = new SparseMatrixRow(vc, idList[i], neighbors.asTroveMap());
                writer.writeRow(row);
            }
        });
        writer.finish();
        SparseMatrix m = new SparseMatrix(tmpMatrix);
        FactorizerUtils.writeTextFormat(m, dest, env.getConfigurator().get(LocalPageDao.class), Language.SIMPLE);
    }

    private TIntSet chooseSample(int numPages) throws ConfigurationException, DaoException {
        TIntIntMap sizes = new TIntIntHashMap();
        RawPageDao pageDao = env.getConfigurator().get(RawPageDao.class);
        for (RawPage page : pageDao.get(DaoFilter.normalPageFilter(lang))) {
            if (page.getTitle().getCanonicalTitle().toLowerCase().contains("list")) {
                continue;
            }
            sizes.put(page.getLocalId(), page.getBody().length());
        }
        int keys[] = WpCollectionUtils.sortMapKeys(sizes, true);
        if (keys.length > numPages) keys = Arrays.copyOfRange(keys, 0, numPages);
        return new TIntHashSet(keys);
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Exporter exp = new Exporter(env);
        for (int n : Arrays.asList(1000, 5000)) {
            exp.export(n, new File(env.getBaseDir(), "/dat/" + n + "-export"));
        }
    }
}
