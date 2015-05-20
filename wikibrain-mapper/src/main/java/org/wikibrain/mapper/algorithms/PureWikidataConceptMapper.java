package org.wikibrain.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
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
import org.wikibrain.parser.sql.MySqlDumpParser;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: bjhecht
 * Date: 6/25/13
 * Time: 1:59 PM
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
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException {
        final Map<Integer, Multimap<Language, LocalId>> backend = Maps.newHashMap();
        final Map<Integer, NameSpace> nsBackend = Maps.newHashMap();

        // loop through sql dump
        MySqlDumpParser dumpParser = new MySqlDumpParser();
        Iterable<Object[]> lines = dumpParser.parse(wikiDataPath);
        int lineCounter = 0; int validLineCounter = 0;
        int[] numLangsCount = new int[ls.size()];

        Set<String> unknownLangs = new HashSet<String>();
        int unknownPages = 0;

        for (Object[] line : lines){
            lineCounter++;
            if (lineCounter % 1000000 == 0){
                LOG.info(String.format("Done with %d total lines of Wikidata dump file", lineCounter));
            }

            String langCode = ((String)line[2]).replaceAll("wiki","");
            if (!Language.hasLangCode(langCode)) {
                unknownLangs.add(langCode);
                continue;
            }
            Language lang = Language.getByLangCode(langCode);
            if (!ls.containsLanguage(lang)){
                continue;
            }
            Integer univId = (Integer)line[1];
            String strTitle = (String)line[3];
            Title title = new Title(strTitle, lang);
            int localId = localPageDao.getIdByTitle(title);
            if (localId <= 0){
                unknownPages++;
                continue;
            }
            if (!backend.containsKey(univId)){
                Multimap<Language, LocalId> mmap = HashMultimap.create();
                backend.put(univId, mmap);
                nsBackend.put(univId, title.getNamespace()); // defines the universal page as having the namespace of the first LocalPage encountered
                numLangsCount[0]++;
            }else{
                numLangsCount[backend.get(univId).size()-1]--;
                numLangsCount[backend.get(univId).size()]++;
            }
            backend.get(univId).put(lang, new LocalId(lang, localId));
            validLineCounter++;

            if (validLineCounter % 10000 == 0){ // do some reporting in the log, necessary for such a large operation (both for debugging and for providing the user with something to watch :-))
                LOG.info("Found " + validLineCounter + " local pages in input language set");
                StringBuilder langDistLine = new StringBuilder();
                langDistLine.append("distribution of pages per # languages: ");
                for(int i = 0; i < numLangsCount.length; i++){
                    langDistLine.append(numLangsCount[i]);
                    langDistLine.append("\t");
                }
                LOG.info(langDistLine.toString());
            }
        }

        LOG.warn("encountered unknown languages: " + unknownLangs);
        LOG.warn("encountered " + unknownPages + " local pages not in the database");

        return new MapperIterator<UniversalPage>(backend.keySet()) {
            @Override
            public UniversalPage transform(Object obj) {
                Integer univId = (Integer)obj;
                return new UniversalPage(univId, getId(), nsBackend.get(univId), backend.get(univId));
            }
        };

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
            List<File> paths = Env.getFiles(Language.WIKIDATA, FileMatcher.WIKIDATA_ITEMS, getConfig());
            if (paths.isEmpty()) {
                throw new ConfigurationException("No wikidata file available for PurWikidataConceptMapper");
            }
            return new PureWikidataConceptMapper(
                    paths.get(0),
                    config.getInt("algorithmId"),
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localPageDao"))
            );
        }
    }
}
