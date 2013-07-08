package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.LocalSRMetric;
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

public class PairwiseSimilarityWriter {
    private static final Logger LOG = Logger.getLogger(PairwiseSimilarityWriter.class.getName());
    private SparseMatrixWriter writer;
    private AtomicInteger idCounter = new AtomicInteger();
    private ValueConf vconf;
    private TIntSet validIds;
    private TIntSet usedIds = new TIntHashSet();
    private LocalSRMetric localSRMetric = null;
    private UniversalSRMetric universalSRMetric = null;
    private Language language;


    public PairwiseSimilarityWriter(File outputFile, LocalSRMetric metric, Language language) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
        this.language = language;
        this.localSRMetric = metric;
    }

    public PairwiseSimilarityWriter(File outputFile, UniversalSRMetric metric) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
        this.universalSRMetric = metric;
    }

    public void setValidIds(TIntSet validIds) {
        this.validIds = validIds;
    }

    public void writeSims(final int wpIds[], final int threads, final int maxSimsPerDoc) throws IOException, InterruptedException, DaoException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : wpIds) { wpIds2.add(id); }
        writeSims(wpIds2, threads, maxSimsPerDoc);
    }

    public void writeSims(List<Integer> wpIds, int threads, final int maxSimsPerDoc) throws IOException, InterruptedException, DaoException {
        ParallelForEach.loop(wpIds, threads, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException, DaoException {
                writeSim(wpId, maxSimsPerDoc);
            }
        }, Integer.MAX_VALUE);
        this.writer.finish();
    }

    private void writeSim(Integer id, int maxSimsPerDoc) throws IOException, DaoException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            String nValidStr  = (validIds == null) ? "infinite" : ("" + validIds.size());
            System.err.println("" + new Date() +
                    ": finding matches for doc " + idCounter.get() +
                    ", used " + usedIds.size() + " of " + nValidStr);
        }
        TIntDoubleMap scores;
        if (localSRMetric!=null){
            scores = localSRMetric.getVector(id, language);
        } else if (universalSRMetric!=null){
            scores = universalSRMetric.getVector(id);
        } else {
            throw new IllegalStateException("PairwiseSimilarityWriter does not have a local or universal metric defined.");
        }
        writer.writeRow(scores);
    }

}

