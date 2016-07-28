package org.wikibrain.cookbook.sr;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.jooq.util.derby.sys.Sys;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
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
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author Qisheng
 * Returns names, vecs, pageviews, pagerank and popularity of full english wikipedia into two files.
 */
public class newFullEnglish {

    public static void main(String[] args) throws Exception     {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao linkDao = conf.get(LocalLinkDao.class);
        PageViewDao pvDao = conf.get(PageViewDao.class);
        Language en = env.getDefaultLanguage();

        DenseVectorSRMetric sr = (DenseVectorSRMetric)conf.get(
                SRMetric.class, "prebuiltword2vec",
                "language", en.getLangCode());
        DenseVectorGenerator gen = sr.getGenerator();

        String path = env.getBaseDir().getCanonicalPath() + "/dat/wikitovec/";

        //Load pageviews
        Map<Language, SortedSet<DateTime>> Hours = pvDao.getLoadedHours();
        SortedSet<DateTime> dt = Hours.values().iterator().next();
        Iterator<DateTime> time = dt.iterator();
        System.out.println("Before get pageviews");
        HashMap<Integer, int[]> dictMap2 = new HashMap<>();

        for (int i = 0; i < 123; i++){
            DateTime s = time.next();
            DateTime e = time.next();
            TIntIntMap all = pvDao.getAllViews(en, s, e);
            for (TIntIntIterator it = all.iterator(); it.hasNext(); ) {
                it.advance();
                if (!dictMap2.containsKey(it.key())) {
                    dictMap2.put(it.key(), new int[123]);
                }
                int[] l = dictMap2.get(it.key());
                l[i] = it.value();
                //System.out.println(l[i]);
                dictMap2.put(it.key(), l);
            }
            System.out.println("Pageviews done by " + i + " times");
        }
        System.out.println("Map size: " + dictMap2.values().size());
        System.out.println("Pageviews done.");

        //Write to files.
        BufferedOutputStream out1 = new BufferedOutputStream(new FileOutputStream(path + "fullvecs.txt"));
        BufferedOutputStream out2 = new BufferedOutputStream(new FileOutputStream(path + "fullnames.txt"));
        BufferedOutputStream out3 = new BufferedOutputStream(new FileOutputStream(path + "fullpageviews.txt"));
        BufferedOutputStream out4 = new BufferedOutputStream(new FileOutputStream(path + "fullpagerank.txt"));
        BufferedOutputStream pop = new BufferedOutputStream(new FileOutputStream(path + "fullPop.txt"));

        BufferedOutputStream out5 = new BufferedOutputStream(new FileOutputStream(path + "outSampleVecs.txt"));
        BufferedOutputStream out6 = new BufferedOutputStream(new FileOutputStream(path + "outSampleNames.txt"));
        BufferedOutputStream out7 = new BufferedOutputStream(new FileOutputStream(path + "outSamplePageview.txt"));
        BufferedOutputStream out8 = new BufferedOutputStream(new FileOutputStream(path + "outSamplePagerank.txt"));
        BufferedOutputStream pop2 = new BufferedOutputStream(new FileOutputStream(path + "outSamplePop.txt"));

        Iterator<LocalPage> iterator = lpDao.get(DaoFilter.normalPageFilter(en)).iterator();
        int ct = 1;
        int count = 1;
        while (iterator.hasNext()){
            LocalPage page = iterator.next();
            int id = page.getLocalId();
            float[] vec = gen.getVector(id);
            if (vec == null){
                System.err.println("Could not find vector for page: " + page);
            }
            else if (!dictMap2.containsKey(id)) {
                System.err.println("Could not find pageview for page: " + page);
            }
            else {
                int[] lst = dictMap2.get(id);
                Arrays.sort(lst);
                int medianPageview = lst[lst.length/2];

                double pr = linkDao.getPageRank(en, page.getLocalId());
                double popularity = medianPageview * pr;
                if (popularity < 1.0E-6){
                    out6.write((Integer.toString(count) + "\t").getBytes());
                    out6.write((page.getTitle().getCanonicalTitle() + "\n").getBytes());
                    out7.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    out7.write((medianPageview + "\n").getBytes());
                    out8.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    out8.write((pr + "\n").getBytes());
                    pop2.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    pop2.write((popularity + "\n").getBytes());
                    out5.write((Integer.toString(count) + "\t").getBytes());
                    for (int i = 0; i < vec.length - 1; i++) {
                        out5.write((Float.toString(vec[i]) + "\t").getBytes());
                    }
                    out5.write((Float.toString(vec[vec.length - 1]) + "\n").getBytes());
                    count++;
                }
                else{
                    out2.write((Integer.toString(ct) + "\t").getBytes());
                    out2.write((page.getTitle().getCanonicalTitle() + "\n").getBytes());
                    out3.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    out3.write((medianPageview + "\n").getBytes());
                    out4.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    out4.write((pr + "\n").getBytes());
                    pop.write((page.getTitle().getCanonicalTitle() + "\t").getBytes());
                    pop.write((popularity + "\n").getBytes());
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
        out3.flush();
        out4.flush();
        out5.flush();
        out6.flush();
        out7.flush();
        out8.flush();
        pop.flush();
        pop2.flush();

    }
}