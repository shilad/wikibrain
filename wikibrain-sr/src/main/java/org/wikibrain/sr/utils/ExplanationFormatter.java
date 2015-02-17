package org.wikibrain.sr.utils;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.sr.Explanation;

import java.util.Map;

/**
 * @author Matt Lesicko
 */
public class ExplanationFormatter {
    LocalPageDao localPageDao;

    public ExplanationFormatter(LocalPageDao localPageDao){
        this.localPageDao=localPageDao;
    }

    public String formatExplanation(Explanation explanation) throws DaoException {
        String[] plaintextBuilder = explanation.getFormat().split("\\?", -1);
        if (explanation.getInformation().size()!=plaintextBuilder.length-1){
            throw new IllegalStateException("Incorrect number of information objects in explanation. Expected "+(plaintextBuilder.length-1)+" but found "+explanation.getInformation().size());
        }
        String plaintext = plaintextBuilder[0];
        for (int i = 1; i<plaintextBuilder.length; i++){
            Object object = explanation.getInformation().get(i-1);

            //Handle the different possible types of information.
            //Add additional handlers as appropriate
            if (object instanceof LocalPage){
                plaintext+=((LocalPage) object).getTitle().getCanonicalTitle();
            }else if(object instanceof UniversalPage){
                Language defaultlang = ((UniversalPage) object).getLanguageSet().getDefaultLanguage();
                LocalId nameId = (LocalId)((UniversalPage) object).getLocalEntities(defaultlang).toArray()[0];
                LocalPage namePage = localPageDao.getById(nameId.getLanguage(), nameId.getId());
                plaintext+=namePage.getTitle().getCanonicalTitle();
            }else {
                plaintext+=object.toString();
            }

            plaintext+=plaintextBuilder[i];
        }
        return plaintext;
    }

    public static class Provider extends org.wikibrain.conf.Provider<ExplanationFormatter> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return ExplanationFormatter.class;
        }

        @Override
        public String getPath() {
            return "sr.explanationformatter";
        }

        @Override
        public ExplanationFormatter get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            return new ExplanationFormatter(
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localpagedao"))
            );
        }
    }
}
