package org.wikapidia.mapper.algorithms;

import org.junit.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht
 * Date: 6/25/13
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestPureWikidataConceptMapper {

    @Test
    public void testBasic(){

        //            Configconfigurator.getConf().get().getList("languages");
//            Language lang = Language.getByLangCode("simple");
//            Title t = new Title("Barack Obama", lang);
//            LocalPage lp = lpDao.getByTitle(lang, t, NameSpace.ARTICLE);
//            System.out.println(lp.getTitle());

        try {

            Env env = new EnvBuilder().setBaseDir(".").build();
            Configurator configurator = new Configurator(new Configuration());
            PureWikidataConceptMapper mapper = (PureWikidataConceptMapper)configurator.get(ConceptMapper.class, "purewikidata");
            MetaInfoDao miDao = configurator.get(MetaInfoDao.class);
            LanguageSet langSet = miDao.getLoadedLanguages();
            LocalPageDao lpDao = configurator.get(LocalPageDao.class);
            System.out.println(langSet);


            Iterator<UniversalPage> uPages = mapper.getConceptMap(langSet);
            while(uPages.hasNext()){
                UniversalPage uPage = uPages.next();
                if (uPage.getNumberOfLanguages() > 1){
                    for (Language lang : langSet){
                        if (uPage.isInLanguage(lang)){
                            LocalId localId = uPage.getLocalEntities(lang).iterator().next();
                            System.out.print(lpDao.getById(localId.getLanguage(),localId.getId()).getTitle().toString() + "\t\t");
                        }
                    }
                    System.out.println();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();  //To chfange body of catch statement use File | Settings | File Templates.
        }


    }
}
