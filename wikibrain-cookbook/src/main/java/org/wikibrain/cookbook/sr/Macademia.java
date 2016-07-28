package org.wikibrain.cookbook.sr;

import gnu.trove.map.TIntIntMap;
import org.apache.commons.io.FileUtils;
import org.clapper.util.io.FileUtil;
import org.joda.time.DateTime;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.DenseVectorGenerator;
import org.wikibrain.sr.vector.DenseVectorSRMetric;
import org.wikibrain.utils.WbMathUtils;

import java.io.*;
import java.util.*;

/**
 * @author Qisheng
 * Required: interest names from Macademia
 * Returns: 200-d vecs, wikipedia titles and the corresponding pageviews
 * (Pageviews can be updated to new measurement of popularity).
 */
public class Macademia {

    public static void main(String[] args) throws Exception     {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Language en = env.getDefaultLanguage();
        PhraseAnalyzer phrases = conf.get(PhraseAnalyzer.class);
        PageViewDao pvDao = conf.get(PageViewDao.class);

        DenseVectorSRMetric sr = (DenseVectorSRMetric)conf.get(
                SRMetric.class, "prebuiltword2vec",
                "language", en.getLangCode());
        DenseVectorGenerator gen = sr.getGenerator();


        String path = env.getBaseDir().getCanonicalPath() + "/dat/wikitovec/";

        BufferedOutputStream out1 = new BufferedOutputStream(new FileOutputStream(path + "interest_vecs.tsv"));
        BufferedOutputStream out2 = new BufferedOutputStream(new FileOutputStream(path + "wiki_titles.tsv"));
        BufferedOutputStream out3 = new BufferedOutputStream(new FileOutputStream(path + "interest_names.tsv"));
        BufferedOutputStream out4 = new BufferedOutputStream(new FileOutputStream(path + "interest_pageviews.txt"));

        Map<String, LocalId> interest = new HashMap<String, LocalId>();

        BufferedReader titles = new BufferedReader(new FileReader("/Users/research/Desktop/interest.txt")); //TODO: change path
        for (String line = titles.readLine(); line != null; line = titles.readLine()){
            //interest.add(line.toLowerCase());
            LinkedHashMap<LocalId, Float> results = phrases.resolve(Language.EN, line.toLowerCase(), 1);
            if (! results.isEmpty()){
                LocalId id = results.entrySet().iterator().next().getKey();
                interest.put(line, id);
            }
        }

        DateTime now = DateTime.now();
        DateTime past = now.minusYears(5);

        for (Map.Entry<String, LocalId> ins: interest.entrySet()){
            LocalPage page = lpDao.getById(ins.getValue());
            int pageview = pvDao.getNumViews(ins.getValue(), past, now);
            float[] vec = gen.getVector(ins.getValue().getId());
            if (vec == null) {
                System.err.println("Could not find vector for page: " + page);
            } else {
                out3.write((ins.getKey() + "\n").getBytes());
                String WikiTitle = page.getTitle().getCanonicalTitle();
                out2.write((WikiTitle + "\n").getBytes());

                out4.write((ins.getKey() + "\t").getBytes());
                out4.write((pageview + "\n").getBytes());

                for (int i = 0; i < vec.length - 1; i++) {
                    out1.write((Float.toString(vec[i]) + "\t").getBytes());
                }
                out1.write((Float.toString(vec[vec.length-1]) + "\n").getBytes());
            }
        }

        out1.flush();
        out2.flush();
        out3.flush();
        out4.flush();
    }

}
