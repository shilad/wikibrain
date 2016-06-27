package org.wikibrain.cookbook.sr;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.DenseVectorGenerator;
import org.wikibrain.sr.vector.DenseVectorSRMetric;
import org.wikibrain.utils.WbMathUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * @author Qisheng
 */
public class WikiToVecFullEn {
    private static final int minPageView = 190;

    public static void main(String[] args) throws Exception     {


        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        PageViewDao pvDao = conf.get(PageViewDao.class);
        Language en = env.getDefaultLanguage();

        DenseVectorSRMetric sr = (DenseVectorSRMetric)conf.get(
                SRMetric.class, "prebuiltword2vec",
                "language", en.getLangCode());
        DenseVectorGenerator gen = sr.getGenerator();


        String path = env.getBaseDir().getCanonicalPath() + "/dat/wikitovec/";

        BufferedOutputStream out1 = new BufferedOutputStream(new FileOutputStream(path + "fullvecs.txt"));
        BufferedOutputStream out2 = new BufferedOutputStream(new FileOutputStream(path + "fullnames.txt"));



        DateTime now = DateTime.now();
        DateTime past = now.minusYears(5);
        TIntIntMap allViews = pvDao.getAllViews(en, past, now);

        int[] count = allViews.values();
        Arrays.sort(count);

        int ct = 1;
        for (TIntIntIterator it = allViews.iterator(); it.hasNext(); ) {
            it.advance();
            if (it.value() > minPageView) {
                LocalPage page = lpDao.getById(en, it.key());
                float[] vec = gen.getVector(page.getLocalId());
                if (vec == null) {
                    System.err.println("Could not find vector for page: " + page);
                } else {
                    out2.write((Integer.toString(ct) + "\t").getBytes());
                    out2.write((page.getTitle().getCanonicalTitle() + "\n").getBytes());
                    out1.write((Integer.toString(ct) + "\t").getBytes());
                    for (int i = 0; i < vec.length - 1; i++) {
                        out1.write((Float.toString(vec[i]) + "\t").getBytes());
                    }
                    out1.write((Float.toString(vec[vec.length - 1]) + "\n").getBytes());
                    ct++;
                }
            }
        }


        out1.flush();
        out2.flush();

//        System.out.println("The sorted int array is:");
//        for (int i = 0; i < 250000; i++) {
//            System.out.println(count[count.length - i -1]);}

    }
}