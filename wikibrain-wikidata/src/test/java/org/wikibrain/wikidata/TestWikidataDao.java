package org.wikibrain.wikidata;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.mockito.Mockito;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.sql.MetaInfoSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
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
        // Add all the entity ids we need as values for SOME key
        URL url = TestWikidataDao.class.getResource("/testDump.json.bz2");

        TIntIntMap map = new TIntIntHashMap();
        BufferedReader reader = WpIOUtils.openBufferedReader(new File(url.toURI()));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (!line.contains("{")) {
                continue;
            }
            line = line.trim();
            if (line.endsWith(",")) {
                line = line.substring(0, line.length()-1);
            }
            JsonElement element = new JsonParser().parse(line.trim());
            String id = element.getAsJsonObject().get("id").getAsString();
            if (id.startsWith("Q")) {
                int i = Integer.valueOf(id.substring(1));
                map.put(i,i);
            }
        }
        reader.close();
        Map<Language, TIntIntMap> concepts = new HashMap<Language, TIntIntMap>();
        for (Language lang : LanguageSet.ALL){
            concepts.put(lang,map);
        }

        UniversalPageDao upDao = Mockito.mock(UniversalPageDao.class);
        Mockito.when(upDao.getAllUnivToLocalIdsMap(LanguageSet.ALL)).thenReturn(concepts);


        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        MetaInfoDao md = new MetaInfoSqlDao(ds);
        md.beginLoad();

        WikidataSqlDao wd = new WikidataSqlDao(ds, null, null);
        wd.beginLoad();

        WikidataDumpLoader loader = new WikidataDumpLoader(wd, md, upDao, LanguageSet.ALL);
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
        assertEquals(props.size(), 1304);
        assertTrue(props.containsKey(127));

        WikidataEntity entity = wd.getProperty(127);
        assertEquals(127, entity.getId());
        assertEquals(WikidataEntity.Type.PROPERTY, entity.getType());

        assertEquals("owned by", entity.getLabels().get(EN));
        assertEquals("propietario", entity.getLabels().get(Language.ES));

        assertEquals("owner of the subject", entity.getDescriptions().get(EN));
        assertTrue(entity.getAliases().get(Language.getByLangCode("cs")).contains("majitel"));
        assertEquals(0, entity.getStatements().size());
    }


    @Test
    public void testItem() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);

        WikidataEntity entity = wd.getItem(23);
        assertEquals(23, entity.getId());
        assertEquals(WikidataEntity.Type.ITEM, entity.getType());

        assertEquals("George Washington", entity.getLabels().get(Language.getByLangCode("en")));
        assertEquals("\u0414\u0436\u043e\u0440\u0434\u0436 \u0412\u0430\u0448\u0438\u043d\u0433\u0442\u043e\u043d", entity.getLabels().get(Language.getByLangCode("ru")));

        assertEquals("American politician, 1st president of the United States (in office from 1789 to 1797)", entity.getDescriptions().get(Language.getByLangCode("en")));
        assertTrue(entity.getAliases().get(Language.getByLangCode("ta")).contains("\u0b9c\u0bcb\u0bb0\u0bcd\u0b9c\u0bcd \u0bb5\u0bca\u0bb7\u0bbf\u0b99\u0bcd\u0b9f\u0ba9\u0bcd"));
        assertEquals(67, entity.getStatements().size());
        Map<String, List<WikidataStatement>> statements = entity.getStatementsInLanguage(Language.getByLangCode("en"));

        assertEquals(2, statements.get("award received").size());
        TIntSet ids = new TIntHashSet();
        for (WikidataStatement st : statements.get("award received")) {
            assertEquals(166, st.getProperty().getId());
            assertEquals("award received", st.getProperty().getLabels().get(EN));
            assertEquals(WikidataValue.Type.ITEM, st.getValue().getType());
            ids.add(st.getValue().getItemValue());
        }
        assertEquals(new TIntHashSet(new int[] {3519573, 721743}), ids);
    }

    @Test
    public void testLocalStatements() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);
        Map<String, List<LocalWikidataStatement>> statements = wd.getLocalStatements(EN, WikidataEntity.Type.ITEM, 23);
        assertEquals(57, statements.keySet().size());

        for (String prop : statements.keySet()) {
            System.out.println("property " + prop + " has statements:");
            for (LocalWikidataStatement st : statements.get(prop)) {
                System.out.println("\t" + st.getFullStatement());
            }
        }

        List<LocalWikidataStatement> occupations = statements.get("occupation");
        assertEquals(4, occupations.size());
        for (LocalWikidataStatement lws : occupations) {
            assertEquals("George Washington occupation unknown", lws.getFullStatement());
        }
    }

    @Test
    public void testSearchForValue() throws Exception {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);
        List<WikidataStatement> stats = IteratorUtils.toList(
                wd.get(new WikidataFilter.Builder().withValue(WikidataValue.forString("11928912p")).build()
            ).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(wd.getByValue("BnF identifier", WikidataValue.forString("11928912p")).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(wd.getByValue(wd.getProperty(268), WikidataValue.forString("11928912p")).iterator());
        assertEquals(1, stats.size());
        stats = IteratorUtils.toList(
                wd.get(new WikidataFilter.Builder().withValue(WikidataValue.forItem(142)).build()
                ).iterator());
        assertEquals(34, stats.size());
        stats = IteratorUtils.toList(wd.getByValue("country of citizenship", WikidataValue.forItem(142)).iterator());
        assertEquals(6, stats.size());
    }

    @Test
    public void testGeoCoordinates() throws Exception {
        WpDataSource ds = TestDaoUtil.getWpDataSource(dbDir);
        WikidataDao wd = new WikidataSqlDao(ds, null, null);
        WikidataFilter filter = (new WikidataFilter.Builder()).withPropertyId(625).build();

        List<WikidataStatement> stats = IteratorUtils.toList(wd.get(filter).iterator());
        assertEquals(190, stats.size());
    }
}
