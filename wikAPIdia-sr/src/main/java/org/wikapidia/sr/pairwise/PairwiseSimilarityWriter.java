package org.wikapidia.sr.pairwise;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 * @author Ben Hillmann
 */

public class PairwiseSimilarityWriter {
    private static final Logger LOG = Logger.getLogger(PairwiseSimilarityWriter.class.getName());

    private SparseMatrixWriter writer;
    private AtomicInteger idCounter = new AtomicInteger();
    private long numCells;
    private ValueConf vconf;
    private TIntSet validIds;
    private TIntSet usedIds = new TIntHashSet();
    private PairwiseCosineSimilarity metric;

    public PairwiseSimilarityWriter(File outputFile, PairwiseCosineSimilarity metric) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
        this.metric  = metric;
    }

    public void setValidIds(TIntSet validIds) {
        this.validIds = validIds;
    }

    public void writeSims(final int wpIds[], final int threads, final int maxSimsPerDoc) throws IOException, InterruptedException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : wpIds) { wpIds2.add(id); }
        writeSims(wpIds2, threads, maxSimsPerDoc);
    }

    public void writeSims(List<Integer> wpIds, int threads, final int maxSimsPerDoc) throws IOException, InterruptedException {
        ParallelForEach.loop(wpIds, threads, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException {
                writeSim(wpId, maxSimsPerDoc);
            }
        }, Integer.MAX_VALUE);
        LOG.info("wrote " + numCells + " non-zero similarity cells");
        this.writer.finish();
    }

    private void writeSim(Integer wpId, int maxSimsPerDoc) throws IOException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            String nValidStr  = (validIds == null) ? "infinite" : ("" + validIds.size());
            System.err.println("" + new Date() +
                    ": finding matches for doc " + idCounter.get() +
                    ", used " + usedIds.size() + " of " + nValidStr);
        }
        SRResultList scores = metric.mostSimilar(wpId, maxSimsPerDoc, validIds);
        if (scores != null) {
            int ids[] = scores.getIds();
            synchronized (this) {
                numCells += scores.getIds().length;
                usedIds.addAll(ids);
            }
            writer.writeRow(new SparseMatrixRow(vconf, wpId, ids, scores.getScoresAsFloat()));
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<PairwiseSimilarityWriter> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PairwiseSimilarityWriter.class;
        }

        @Override
        public String getPath() {
            return "matrix.feature";
        }

        @Override
        public PairwiseSimilarityWriter get(String name, Config config) throws ConfigurationException {
            if (config.getString("type").equals("local")) {
                return null;
            }
        }
    }


}
