package org.wikapidia.sr;

import com.jolbox.bonecp.BoneCPDataSource;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import org.apache.commons.collections.CollectionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;
import org.wikapidia.matrix.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.disambig.TopResultDisambiguator;
import org.wikapidia.sr.pairwise.PairwiseCosineSimilarity;
import org.wikapidia.sr.pairwise.PairwiseSimilarityWriter;

 import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestPairwiseSimilarity {
    static int NUM_ROWS = 6;

    private static LocalMilneWitten srIn;

    private static SparseMatrix matrix;
    private static SparseMatrix transpose;

    @BeforeClass
    public static void createTestData() throws IOException, ClassNotFoundException, DaoException {// Create test data and transpose
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        LanguageInfo lang = LanguageInfo.getByLangCode("simple");
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        LocalLinkSqlDao linkDao = new LocalLinkSqlDao(ds);

        dao.beginLoad();
        LocalPage page1 = new LocalPage(
                lang.getLanguage(),
                1,
                new Title("test1", lang),
                NameSpace.ARTICLE
        );
        dao.save(page1);
        LocalPage page2 = new LocalPage(
                lang.getLanguage(),
                2,
                new Title("test2", lang),
                NameSpace.ARTICLE
        );
        dao.save(page2);
        LocalPage page3 = new LocalPage(
                lang.getLanguage(),
                3,
                new Title("test3", lang),
                NameSpace.ARTICLE
        );
        dao.save(page3);
        LocalPage page4 = new LocalPage(
                lang.getLanguage(),
                4,
                new Title("test4", lang),
                NameSpace.ARTICLE
        );
        dao.save(page4);
        LocalPage page5 = new LocalPage(
                lang.getLanguage(),
                5,
                new Title("test5", lang),
                NameSpace.ARTICLE
        );
        dao.save(page5);
        LocalPage page6 = new LocalPage(
                lang.getLanguage(),
                6,
                new Title("test6", lang),
                NameSpace.ARTICLE
        );
        dao.save(page6);
        dao.endLoad();

        linkDao.beginLoad();
        LocalLink link1 = new LocalLink(lang.getLanguage(), "", 3, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link1);
        LocalLink link2 = new LocalLink(lang.getLanguage(), "", 4, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link2);
        LocalLink link3 = new LocalLink(lang.getLanguage(), "", 4, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link3);
        LocalLink link4 = new LocalLink(lang.getLanguage(), "", 5, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link4);
        LocalLink link5 = new LocalLink(lang.getLanguage(), "", 6, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link5);
        LocalLink link6 = new LocalLink(lang.getLanguage(), "", 5, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link6);
        LocalLink link7 = new LocalLink(lang.getLanguage(), "", 6, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link7);
        linkDao.endLoad();

        List<SparseMatrixRow> rows = new ArrayList<SparseMatrixRow>();
        DaoFilter pageFilter = new DaoFilter().setLanguages(Language.getByLangCode("simple"));


        Disambiguator disambiguator = new TopResultDisambiguator(null);

        srIn = new LocalMilneWitten(disambiguator,linkDao,dao);

        File tmpFile = File.createTempFile("matrix", null);
        tmpFile.deleteOnExit();
        SparseMatrixWriter writer = new SparseMatrixWriter(tmpFile, new ValueConf());

        Iterable<LocalArticle> articles = dao.get(pageFilter);

        for (LocalArticle article : articles) {
            TIntDoubleMap vector = srIn.getVector(article.getLocalId(), article.getLanguage());
            System.out.println(vector);
            LinkedHashMap<Integer,Float> linkedHashMap = new LinkedHashMap<Integer, Float>();
            for (int i : vector.keys()){
                linkedHashMap.put(i,(float)vector.get(i));
            }

            SparseMatrixRow row = new SparseMatrixRow(new ValueConf(), article.getLocalId(), linkedHashMap);
            System.out.println(row.asMap());
            writer.writeRow(row);
        }

        writer.finish();

        matrix = new SparseMatrix(tmpFile);

        System.out.println(matrix.getRow(1).asMap());

        new SparseMatrixTransposer(matrix, tmpFile, 10).transpose();
        transpose = new SparseMatrix(tmpFile);

    }

    @Test
    public void testSimilarity() throws IOException, InterruptedException, DaoException, ClassNotFoundException, WikapidiaException {


        File simPath = File.createTempFile("matrix", null);
        simPath.deleteOnExit();

        PairwiseCosineSimilarity cosine = new PairwiseCosineSimilarity(matrix, transpose);
        PairwiseSimilarityWriter writer = new PairwiseSimilarityWriter(simPath,srIn, Language.getByLangCode("simple"));
        writer.writeSims(matrix.getRowIds(), 1, NUM_ROWS);
        SparseMatrix sims = new SparseMatrix(simPath);

        // Calculate similarities by hand
        TLongDoubleHashMap dot = new TLongDoubleHashMap();
        TIntDoubleHashMap len2 = new TIntDoubleHashMap();

        for (SparseMatrixRow row1 : matrix) {

            Map<Integer, Float> data1 = row1.asMap();
            int id1 = row1.getRowIndex();

            // Calculate the length^2
            double len = 0.0;
            for (double val : data1.values()) {
                len += val * val;
            }
            len2.put(id1, len);

            for (SparseMatrixRow row2 : matrix) {
                int id2 = row2.getRowIndex();
                Map<Integer, Float> data2 = row2.asMap();
                double sim = 0.0;

                for (Object key : CollectionUtils.intersection(data1.keySet(), data2.keySet())) {
                    sim += data1.get(key) * data2.get(key);
                }
                if (sim != 0) {
                    dot.put(pack(id1, id2), sim);
                }
            }
        }

        int numCells = 0;
        for (MatrixRow row : sims) {
            for (int i = 0; i < row.getNumCols(); i++) {
                if (row.getColValue(i) != 0) {
                    int id1 = row.getRowIndex();
                    int id2 = row.getColIndex(i);
                    numCells++;
                    double xDotX = len2.get(id1);
                    double yDotY = len2.get(id2);
                    double xDotY = dot.get(pack(id1, id2));
                    System.out.println(sims.getRow(1).asMap());
                    System.out.println(sims.getRow(2).asMap());
                    System.out.println(id1);
                    System.out.println(id2);
                    System.out.println(xDotY / Math.sqrt(xDotX * yDotY));

//                    assertEquals(row.getColValue(i), xDotY / Math.sqrt(xDotX * yDotY), 0.001);
                }
            }
        }
        assertEquals(numCells, dot.size());
    }

    @Test
    public void testSimilarityMatchesMostSimilar() throws IOException {
        PairwiseCosineSimilarity cosine = new PairwiseCosineSimilarity(matrix, transpose);
        int[] ids = matrix.getRowIds();
        Map<Integer, TIntDoubleHashMap> sims = new HashMap<Integer, TIntDoubleHashMap>();
        for (int id : ids) {
            sims.put(id, new TIntDoubleHashMap());
            for (SRResult score : cosine.mostSimilar(id, NUM_ROWS, null)) {
                sims.get(id).put(score.getId(), score.getValue());
            }
        }
        for (int id1 : ids) {
            for (int id2 : ids) {
                double s = cosine.similarity(id1, id2);
                if (sims.containsKey(id1) && sims.get(id1).containsKey(id2)) {
                    assertEquals(s, sims.get(id1).get(id2), 0.001);
                } else {
                    assertEquals(s, 0.0, 0.0001);
                }
            }
        }
    }

    private long pack(int x, int y) {
        return ByteBuffer.wrap(new byte[8]).putInt(x).putInt(y).getLong(0);
    }
}
