package org.wikibrain.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.apache.commons.io.LineIterator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.ConceptMapper;
import org.wikibrain.mapper.MapperIterator;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * User: bjhecht, Shilad Sen
 */
public class PureWikidataConceptMapper extends ConceptMapper {

    private static Logger LOG = LoggerFactory.getLogger(PureWikidataConceptMapper.class);
    private final File wikiDataPath;

    public PureWikidataConceptMapper(File wikiDataPath, int id, LocalPageDao localPageDao) {
        super(id, localPageDao);
        this.wikiDataPath = wikiDataPath;
    }

    @Override
    public int getId() {
        return super.getId();
    }



    @Override
    public Iterator<UniversalPage> getConceptMap(final LanguageSet ls) throws DaoException {
        LineIterator lines = null;
        try {
            lines = new LineIterator(WpIOUtils.openBufferedReader(wikiDataPath));
        } catch (IOException e) {
            throw new DaoException(e);
        }
        final Map<Integer, Multimap<Language, LocalId>> backend = Maps.newHashMap();
        final Map<Integer, NameSpace> nsBackend = Maps.newHashMap();
        final ThreadLocal<JSONParser> parser = new ThreadLocal<JSONParser>();

        final AtomicInteger lineCounter = new AtomicInteger();
        final AtomicInteger validLineCounter = new AtomicInteger();
        final AtomicInteger unknownPages = new AtomicInteger();
        final AtomicIntegerArray numLangsCount = new AtomicIntegerArray(ls.size() + 1);


        ParallelForEach.iterate(lines, new Procedure<String>() {
            @Override
            public void call(String line) throws Exception {
                lineCounter.incrementAndGet();
                if (lineCounter.get() % 50000 == 0){ // do some reporting in the log, necessary for such a large operation (both for debugging and for providing the user with something to watch :-))
                    LOG.info(
                            "Found " + lineCounter.get() +
                            " lines, " + validLineCounter.get() +
                            " valid lines, local pages in input language set:" +
                            numLangsCount);
                }

                line = line.trim();
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line.trim();
                if (line.isEmpty()) return;
                if (!line.startsWith("{") || !line.endsWith("}")) {
                    LOG.info("Invalid line: " + line);
                    return;
                }
                if (parser.get() == null) parser.set(new JSONParser());
                JSONObject obj = (JSONObject) parser.get().parse(line);
                if (!obj.containsKey("id")) return;
                String idStr = (String) obj.get("id");
                if (!idStr.startsWith("Q")) return;
                int id = Integer.valueOf(idStr.substring(1));

                if (!obj.containsKey("sitelinks")) return;
                JSONObject links = (JSONObject) obj.get("sitelinks");
                NameSpace ns = null;
                Multimap<Language, LocalId> mm = HashMultimap.create();

                validLineCounter.incrementAndGet();
                for (Language l : ls) {
                    String key = l.getLangCode() + "wiki";
                    if (!links.containsKey(key)) continue;
                    JSONObject info = (JSONObject) links.get(key);
                    Title title = new Title((String) info.get("title"), l);
                    ns = title.getNamespace();

                    int localId = localPageDao.getIdByTitle(title);
                    if (localId <= 0){
                        unknownPages.incrementAndGet();
                        continue;
                    }

                    mm.put(l, new LocalId(l, localId));
                }

                if (!mm.isEmpty()) {
                    synchronized (backend) {
                        backend.put(id, mm);
                        nsBackend.put(id, ns);
                    }
                }
                numLangsCount.incrementAndGet(mm.size());

            }
        });

        LOG.warn("encountered " + unknownPages.get() + " local pages not in the database");

        return new MapperIterator<UniversalPage>(backend.keySet()) {
            @Override
            public UniversalPage transform(Object obj) {
                Integer univId = (Integer)obj;
                return new UniversalPage(univId, getId(), nsBackend.get(univId), backend.get(univId));
            }
        };

    }

    public static File getJsonLocation(Configuration conf) {
        return new File(conf.getFile("download.path"), "wikidata.json.bz2");
    }

    public static class Provider extends org.wikibrain.conf.Provider<ConceptMapper> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return ConceptMapper.class;
        }

        @Override
        public String getPath() {
            return "mapper";
        }

        @Override
        public ConceptMapper get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("purewikidata")) {
                return null;
            }
            File path = getJsonLocation(getConfig());
            if (!path.isFile()) {
                throw new ConfigurationException("No wikidata file available for PurWikidataConceptMapper: " + path);
            }
            return new PureWikidataConceptMapper(
                    path,
                    config.getInt("algorithmId"),
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localPageDao"))
            );
        }
    }
}
