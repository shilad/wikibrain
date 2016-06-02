package org.wikibrain.cookbook.sr;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.DenseVectorGenerator;
import org.wikibrain.sr.vector.DenseVectorSRMetric;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * @author Jaco
 */
public class WikiToVec {
    public static void main(String[] args) throws Exception     {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Language simple = Language.getByLangCode("simple");

        DenseVectorSRMetric sr = (DenseVectorSRMetric)conf.get(
                SRMetric.class, "word2smallvec",
                "language", simple.getLangCode());
        DenseVectorGenerator gen = sr.getGenerator();

        String path = env.getBaseDir().getCanonicalPath() + "/dat/wikitovec/";

        BufferedOutputStream out1 = new BufferedOutputStream(new FileOutputStream(path + "vecs.txt"));
        BufferedOutputStream out2 = new BufferedOutputStream(new FileOutputStream(path + "urls.txt"));
        for (LocalPage p : lpDao.get(DaoFilter.normalPageFilter(Language.SIMPLE))) {
            out2.write((p.getCompactUrl() + "\n").getBytes());
            float[] vec = gen.getVector(p.getLocalId());
            for (int i = 0; i < vec.length-1; i++) {
                out1.write((Float.toString(vec[i]) + ",").getBytes());
            }
            out1.write((Float.toString(vec[vec.length-1]) + "\n").getBytes());
        }
        out1.flush();
        out2.flush();
    }
}
