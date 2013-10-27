package org.wikapidia.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.FileMatcher;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.MapperIterator;
import org.wikapidia.parser.sql.MySqlDumpParser;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: bjhecht
 * Date: 6/25/13
 * Time: 1:59 PM
 */
public class PureWikidataConceptMapper extends ConceptMapper {

    private static Logger LOG = Logger.getLogger(PureWikidataConceptMapper.class.getName());
    private final File wikiDataPath;

    protected PureWikidataConceptMapper(File wikiDataPath, int id, LocalPageDao localPageDao) {
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
        for (Object[] line : lines){
            String langCode = ((String)line[2]).replaceAll("wiki","");
            try{
                Language lang = Language.getByLangCode(langCode);
                if (ls.containsLanguage(lang)){
                    Integer univId = (Integer)line[1];
                    String strTitle = (String)line[3];
                    Title title = new Title(strTitle, lang);
                    LocalPage localPage = localPageDao.getByTitle(lang, title, title.getNamespace());
                    if (localPage != null){
                        if (!backend.containsKey(univId)){
                            Multimap<Language, LocalId> mmap = HashMultimap.create();
                            backend.put(univId, mmap);
                            nsBackend.put(univId, localPage.getNameSpace()); // defines the universal page as having the namespace of the first LocalPage encountered
                            numLangsCount[0]++;
                        }else{
                            numLangsCount[backend.get(univId).size()-1]--;
                            numLangsCount[backend.get(univId).size()]++;
                        }
                        backend.get(univId).put(lang, localPage.toLocalId());
                        validLineCounter++;
                        if (validLineCounter % 1000 == 0){ // do some reporting in the log, necessary for such a large operation (both for debugging and for providing the user with something to watch :-))
                            LOG.info("Found " + validLineCounter + " local pages in input language set");
                            StringBuilder langDistLine = new StringBuilder();
                            langDistLine.append("distribution of pages per # languages: ");
                            for(int i = 0; i < numLangsCount.length; i++){
                                langDistLine.append(numLangsCount[i]);
                                langDistLine.append("\t");
                            }
                            LOG.info(langDistLine.toString());
                        }
                    }else{
                        LOG.info("Found a local page in the wikidata file that is not in the LocalPageDao: " +
                                strTitle + " (" + langCode + ")");
                    }
                }
            }catch(IllegalArgumentException e){
                //occurs when there is a language in the Wikidata file that is not in the list of languages supported by the WikAPIdia software
                LOG.finest("Found language in Wikidata file that is not supported by wikAPIdia: " + langCode);
            }
            lineCounter++;
            if (lineCounter % 1000000 == 0){
                LOG.info(String.format("Done with %d total lines of Wikidata dump file", lineCounter));
            }
        }

        return new MapperIterator<UniversalPage>(backend.keySet()) {
            @Override
            public UniversalPage transform(Object obj) {
                Integer univId = (Integer)obj;
                return new UniversalPage(univId, getId(), nsBackend.get(univId), backend.get(univId));
            }
        };

    }

    public static class Provider extends org.wikapidia.conf.Provider<ConceptMapper> {
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
        public ConceptMapper get(String name, Config config) throws ConfigurationException {
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
