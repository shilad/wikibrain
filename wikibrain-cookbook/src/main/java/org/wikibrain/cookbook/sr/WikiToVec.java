package org.wikibrain.cookbook.sr;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.DenseVectorGenerator;
import org.wikibrain.sr.vector.DenseVectorSRMetric;
import org.wikibrain.utils.WbMathUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Jaco
 */
public class WikiToVec {
    private static final String [] TOP_LEVEL_CATS = {"Geography", "History", "Knowledge", "People", "Religion", "Science"};
    private static final int numPages = 8000;
    private static final int VEC_LENGTH = 50;

    public static void main(String[] args) throws Exception     {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalCategoryMemberDao catDao = env.getConfigurator().get(LocalCategoryMemberDao.class);
        Language simple = Language.getByLangCode("simple");

        DenseVectorSRMetric sr = (DenseVectorSRMetric)conf.get(
                SRMetric.class, "word2smallvec",
                "language", simple.getLangCode());
        DenseVectorGenerator gen = sr.getGenerator();

        // catVecs based on articles
        /*float[][] catVecs = new float[TOP_LEVEL_CATS.length][VEC_LENGTH];
        for (int i = 0; i < TOP_LEVEL_CATS.length; i++) {
            LocalPage p = lpDao.getByTitle(Language.SIMPLE, TOP_LEVEL_CATS[i]);
            catVecs[i] = gen.getVector(p.getLocalId());
        }*/

        float[][] catVecs = makeCatVecs(lpDao, sr, catDao);
        String path = env.getBaseDir().getCanonicalPath() + "/dat/wikitovec/";

        BufferedOutputStream out1 = new BufferedOutputStream(new FileOutputStream(path + "vecs.txt"));
        BufferedOutputStream out2 = new BufferedOutputStream(new FileOutputStream(path + "urls.txt"));
        BufferedOutputStream out3 = new BufferedOutputStream(new FileOutputStream(path + "cats.txt"));
        Iterator<LocalPage> iterator = lpDao.get(DaoFilter.normalPageFilter(Language.SIMPLE)).iterator();
        for (int p = 0; p < numPages; p++) {
            LocalPage page = iterator.next();
            out2.write((page.getTitle().getCanonicalTitle() + "\n").getBytes());
            float[] vec = gen.getVector(page.getLocalId());
            for (int i = 0; i < vec.length-1; i++) {
                out1.write((Float.toString(vec[i]) + ",").getBytes());
            }
            out1.write((Float.toString(vec[vec.length-1]) + "\n").getBytes());
            out3.write((TOP_LEVEL_CATS[findMostSimilar(catVecs, vec)] + "\n").getBytes());
        }
        out1.flush();
        out2.flush();
        out3.flush();
    }

    private static int findMostSimilar(float[][] catVecs, float[] vec) {
        double minProd = WbMathUtils.dot(catVecs[0], vec);
        int idx = 0;
        for (int i = 0; i < catVecs.length; i++) {
            double prod = WbMathUtils.dot(catVecs[i], vec);
            if (prod <= minProd) {
                idx = i;
            }
        }
        return idx;
    }

    private static float[][] makeCatVecs(LocalPageDao dao, DenseVectorSRMetric sr, LocalCategoryMemberDao catDao) throws Exception {
        float[][] catVecs = new float[TOP_LEVEL_CATS.length][VEC_LENGTH];
        for (int i = 0; i < TOP_LEVEL_CATS.length; i++) {
            LocalPage p = dao.getByTitle(Language.SIMPLE, NameSpace.CATEGORY, "Category:" + TOP_LEVEL_CATS[i]);
            Map<Integer, LocalPage> members = catDao.getCategoryMembers(p);
            if (members != null) {
                int nArticles = 0;
                for (LocalPage page : members.values()) {
                    float[] v = sr.getPageVector(page.getLocalId());
                    if (v != null && page.getNameSpace() == NameSpace.ARTICLE) {
                        for (int j = 0; j < v.length; j++) catVecs[i][j] += v[j];
                        nArticles++;
                    }
                }
                for (int j = 0; j < catVecs[i].length; j++) {
                    catVecs[i][j] /= nArticles;
                }
            }
        }
        return catVecs;
    }
}