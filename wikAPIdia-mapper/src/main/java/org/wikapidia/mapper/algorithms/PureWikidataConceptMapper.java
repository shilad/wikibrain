package org.wikapidia.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.typesafe.config.Config;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.MapperIterator;
import org.wikapidia.parser.sql.MySqlDumpParser;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht
 * Date: 6/25/13
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */


public class PureWikidataConceptMapper extends ConceptMapper {

    private static final String WIKIDATA_MAPPING_FILE_PATH = "/Users/bjhecht/Downloads/wikidatawiki-20130527-wb_items_per_site.sql";

    protected PureWikidataConceptMapper(int id, LocalPageDao<LocalPage> localPageDao) {
        super(id, localPageDao);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int getId() {
        return super.getId();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException {
//        UniversalPage up = new UniversalPage(

        File wikiDataDumpFile = new File(WIKIDATA_MAPPING_FILE_PATH);

        final Map<Integer, Multimap<Language, LocalId>> backend = Maps.newHashMap();
        final Map<Integer, NameSpace> nsBackend = Maps.newHashMap();

        // loop through sql dump
        MySqlDumpParser dumpParser = new MySqlDumpParser();
        Iterable<Object[]> lines = dumpParser.parse(wikiDataDumpFile);
        for (Object[] line : lines){
            String langCode = ((String)line[2]).replaceAll("wiki","");
            Language lang = Language.getByLangCode(langCode);
            if (ls.containsLanguage(lang)){
                Integer localId = (Integer)line[0];
                Integer univId = (Integer)line[1];
                LocalPage localPage = localPageDao.getById(lang, localId);
                if (!backend.containsKey(univId)){
                    Multimap<Language, LocalId> mmap = HashMultimap.create();
                    backend.put(univId, mmap);
                    nsBackend.put(univId, localPage.getNameSpace()); // defines the universal page as having the namespace of the first LocalPage encountered
                }
                backend.get(univId).put(lang, localPage.toLocalId());
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
            return new PureWikidataConceptMapper(
                    config.getInt("algorithmId"),
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localPageDao"))
            );
        }
    }
}
