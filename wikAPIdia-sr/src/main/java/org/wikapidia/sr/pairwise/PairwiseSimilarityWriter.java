package org.wikapidia.sr.pairwise;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.LocalSRMetric;
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
    private LocalSRMetric metric;
    private Language language;

    public PairwiseSimilarityWriter(File outputFile) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
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
        this.writer.finish();
    }

    private void writeSim(Integer wpId, int maxSimsPerDoc) throws IOException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            String nValidStr  = (validIds == null) ? "infinite" : ("" + validIds.size());
            System.err.println("" + new Date() +
                    ": finding matches for doc " + idCounter.get() +
                    ", used " + usedIds.size() + " of " + nValidStr);
        }
        SparseMatrixRow scores = metric.getVector(wpId, language);
        writer.writeRow(scores);
    }
}
