package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.*;
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
    private String path;


    public SRFeatureMatrixWriter(String path, LocalSRMetric metric, Language language) throws IOException {
        this.vconf = new ValueConf();
        this.path = path;
        this.writer = new SparseMatrixWriter(new File(path + "-feature"), vconf);
        this.language = language;
        this.localSRMetric = metric;
    }

    public SRFeatureMatrixWriter(String path, UniversalSRMetric metric) throws IOException {
        this.vconf = new ValueConf();
        this.path = path;
        this.writer = new SparseMatrixWriter(new File(path + "-feature"), vconf);
        this.universalSRMetric = metric;
    }

    public void writeFeatureVectors(final int wpIds[], final int threads) throws WikapidiaException, InterruptedException, IOException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : wpIds) { wpIds2.add(id); }
        writeFeatureVectors(wpIds2, threads);
    }

    public void writeFeatureVectors(List<Integer> wpIds, int threads) throws WikapidiaException, InterruptedException, IOException {
        ParallelForEach.loop(wpIds, threads, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException, DaoException, WikapidiaException {
                writeFeatureVector(wpId);
            }
        }, 10000);
        try {
            this.writer.finish();
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
        writeTranspose();
    }

    public void writeTranspose() throws IOException {
        SparseMatrixTransposer transposer = new SparseMatrixTransposer(new SparseMatrix(new File(path + "-feature")),
                new File(path + "-transpose"),
                1024*1024*500); //500mb buffer
        transposer.transpose();
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

