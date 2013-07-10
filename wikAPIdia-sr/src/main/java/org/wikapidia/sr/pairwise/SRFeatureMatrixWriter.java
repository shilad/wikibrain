package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */

public class SRFeatureMatrixWriter {
    private SparseMatrixWriter writer;
    private ValueConf vconf;
    private LocalSRMetric localSRMetric = null;
    private UniversalSRMetric universalSRMetric = null;
    private Language language;


    public SRFeatureMatrixWriter(File outputFile, LocalSRMetric metric, Language language) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
        this.language = language;
        this.localSRMetric = metric;
    }

    public SRFeatureMatrixWriter(File outputFile, UniversalSRMetric metric) throws IOException {
        this.vconf = new ValueConf();
        this.writer = new SparseMatrixWriter(outputFile, vconf);
        this.universalSRMetric = metric;
    }

    public void writeFeatureVectors(final int wpIds[], final int threads, int NUM_ROWS) throws WikapidiaException, InterruptedException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : wpIds) { wpIds2.add(id); }
        writeFeatureVectors(wpIds2, threads);
    }

    public void writeFeatureVectors(List<Integer> wpIds, int threads) throws WikapidiaException, InterruptedException{
        ParallelForEach.loop(wpIds, threads, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException, DaoException, WikapidiaException {
                writeFeatureVector(wpId);
            }
        }, Integer.MAX_VALUE);
        try {
            this.writer.finish();
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    private void writeFeatureVector(Integer id) throws WikapidiaException {
        TIntDoubleMap scores;
        try {
            if (localSRMetric!=null){
                scores = localSRMetric.getVector(id, language);
            } else if (universalSRMetric!=null){
                scores = universalSRMetric.getVector(id);
            } else {
                throw new IllegalStateException("SRFeatureMatrixWriter does not have a local or universal metric defined.");
            }
        } catch (DaoException e){
            throw new WikapidiaException(e);
        }
        LinkedHashMap<Integer,Float> linkedHashMap = new LinkedHashMap<Integer, Float>();
        for (int i : scores.keys()){
            linkedHashMap.put(i,(float)scores.get(i));
        }
        try {
            writer.writeRow(new SparseMatrixRow(new ValueConf(), id, linkedHashMap));
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

}

