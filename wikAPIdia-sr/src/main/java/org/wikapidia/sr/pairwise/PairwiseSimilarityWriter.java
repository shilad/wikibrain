package org.wikapidia.sr.pairwise;

import edu.macalester.wpsemsim.matrix.SparseMatrixRow;
import edu.macalester.wpsemsim.matrix.SparseMatrixWriter;
import edu.macalester.wpsemsim.matrix.ValueConf;
import edu.macalester.wpsemsim.sim.SimilarityMetric;
import edu.macalester.wpsemsim.utils.DocScoreList;
import edu.macalester.wpsemsim.utils.Procedure;
import edu.macalester.wpsemsim.utils.ParallelForEach;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class PairwiseSimilarityWriter {
    private static final Logger LOG = Logger.getLogger(PairwiseSimilarityWriter.class.getName());

    private SimilarityMetric metric;
    private SparseMatrixWriter writer;
    private AtomicInteger idCounter = new AtomicInteger();
    private long numCells;
    private ValueConf vconf;
    private TIntSet validIds;
    private TIntSet usedIds = new TIntHashSet();

    public PairwiseSimilarityWriter(SimilarityMetric metric, File outputFile) throws IOException {
        this.metric = metric;
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
        DocScoreList scores = metric.mostSimilar(wpId, maxSimsPerDoc, validIds);
        if (scores != null) {
            int ids[] = scores.getIds();
            synchronized (this) {
                numCells += scores.getIds().length;
                usedIds.addAll(ids);
            }
            writer.writeRow(new SparseMatrixRow(vconf, wpId, ids, scores.getScoresAsFloat()));
        }
    }
}
