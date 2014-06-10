package org.wikibrain.wikidata;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.sql.MetaInfoSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestWikidataDao {
    public static Language EN = Language.getByLangCode("en");

    public static File cacheFile;
    public static File dbDir;

    @BeforeClass
    public static void createDb() throws IOException, DaoException, ClassNotFoundException, URISyntaxException {
        dbDir = File.createTempFile("dao", "cache");
        dbDir.delete();
        dbDir.mkdirs();

        cacheFile = File.createTempFile("dao", "cache");
        cacheFile.delete();
        cacheFile.mkdirs();

        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        MetaInfoDao md = new MetaInfoSqlDao(ds);
        md.beginLoad();

        WikidataSqlDao wd = new WikidataSqlDao(ds, null, null);
        wd.beginLoad();

        WikidataDumpLoader loader = new WikidataDumpLoader(wd, md, LanguageSet.ALL);
        URL url = TestWikidataDao.class.getResource("/testDump.xml.bz2");
        loader.load(new File(url.toURI()));
        wd.endLoad();
        md.endLoad();

    }

    @AfterClass
    public static void deleteDb() throws IOException {
        FileUtils.deleteDirectory(dbDir);
        FileUtils.deleteDirectory(cacheFile);
    }

    @Test
    public void testProps() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);

        Map<Integer, WikidataEntity> props = wd.getProperties();
        assertEquals(props.size(), 836);
        assertTrue(props.containsKey(127));

        WikidataEntity entity = wd.getProperty(127);
        assertEquals(127, entity.getId());
        assertEquals(WikidataEntity.Type.PROPERTY, entity.getType());

        assertEquals("owned by", entity.getLabels().get(EN));
        assertEquals("propietario", entity.getLabels().get(Language.getByLangCode("es")));

        assertEquals("owner of the subject", entity.getDescriptions().get(EN));
        assertTrue(entity.getAliases().get(Language.getByLangCode("cs")).contains("majitel"));
        assertEquals(0, entity.getStatements().size());
    }


    @Test
    public void testItem() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);

        WikidataEntity entity = wd.getItem(157);
        assertEquals(157, entity.getId());
        assertEquals(WikidataEntity.Type.ITEM, entity.getType());

        assertEquals("Fran\u00e7ois Hollande", entity.getLabels().get(Language.getByLangCode("en")));
        assertEquals("\u041e\u043b\u043b\u0430\u043d\u0434, \u0424\u0440\u0430\u043d\u0441\u0443\u0430", entity.getLabels().get(Language.getByLangCode("ru")));

        assertEquals("24th President of the French Republic", entity.getDescriptions().get(Language.getByLangCode("en")));
        assertTrue(entity.getAliases().get(Language.getByLangCode("ca")).contains("Hollande"));
        assertEquals(36, entity.getStatements().size());
        Map<String, List<WikidataStatement>> statements = entity.getStatementsInLanguage(Language.getByLangCode("en"));

        assertEquals(4, statements.get("award received").size());
        TIntSet ids = new TIntHashSet();
        for (WikidataStatement st : statements.get("award received")) {
            assertEquals(166, st.getProperty().getId());
            assertEquals("award received", st.getProperty().getLabels().get(EN));
            assertEquals(WikidataValue.Type.ITEM, st.getValue().getType());
            ids.add(st.getValue().getItemValue());
        }
        assertEquals(new TIntHashSet(new int[] {84020, 10855226, 13422143, 14539990}), ids);
    }

    @Test
    public void testLocalStatements() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);
        Map<String, List<LocalWikidataStatement>> statements = wd.getLocalStatements(EN, WikidataEntity.Type.ITEM, 157);
        assertEquals(25, statements.keySet().size());

        for (String prop : statements.keySet()) {
            System.out.println("property " + prop + " has statements:");
            for (LocalWikidataStatement st : statements.get(prop)) {
                System.out.println("\t" + st.getFullStatement());
            }
        }

        List<LocalWikidataStatement> almaMaters = statements.get("alma mater");
        assertEquals(4, almaMaters.size());
        for (LocalWikidataStatement lws : almaMaters) {
            assertEquals("François Hollande alma mater unknown", lws.getFullStatement());
        }
    }

    @Test
    public void testSearchForValue() throws Exception {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);
        List<WikidataStatement> stats = IteratorUtils.toList(
                wd.get(new WikidataFilter.Builder().withValue(WikidataValue.forString("122353562")).build()
            ).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(wd.getByValue("BnF identifier", WikidataValue.forString("122353562")).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(wd.getByValue(wd.getProperty(268), WikidataValue.forString("122353562")).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(
                wd.get(new WikidataFilter.Builder().withValue(WikidataValue.forItem(142)).build()
                ).iterator());
        assertEquals(30, stats.size());
        stats = IteratorUtils.toList(wd.getByValue("country of citizenship", WikidataValue.forItem(142)).iterator());
        assertEquals(5, stats.size());
    }
}
