package org.wikapidia.sr.pairwise;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private PairwiseSimilarity metric;



    public PairwiseSimilarityWriter(String path, PairwiseSimilarity metric) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(new File(path+"-cosimilarity"), vconf);
        this.metric  = metric;
    }

    public void setValidIds(TIntSet validIds) {
        this.validIds = validIds;
    }

    public void writeSims(final int wpIds[], final int maxSimsPerDoc) throws IOException, InterruptedException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : wpIds) { wpIds2.add(id); }
        writeSims(wpIds2, maxSimsPerDoc);
    }

    public void writeSims(List<Integer> wpIds, final int maxSimsPerDoc) throws IOException, InterruptedException {
        ParallelForEach.loop(wpIds, new Procedure<Integer>() {
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
            LOG.info("finding matches for doc " + idCounter.get() +
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
}
