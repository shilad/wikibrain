package org.wikapidia.sr.utils;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.sr.Explanation;

import javax.sql.DataSource;

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
                LocalId nameId = (LocalId)((UniversalPage) object).getLocalPages(defaultlang).toArray()[0];
                LocalPage namePage = localPageDao.getById(nameId.getLanguage(), nameId.getId());
                plaintext+=namePage.getTitle().getCanonicalTitle();
            }else {
                plaintext+=object.toString();
            }

            plaintext+=plaintextBuilder[i];
        }
        return plaintext;
    }

    public static class Provider extends org.wikapidia.conf.Provider<ExplanationFormatter> {
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
        public ExplanationFormatter get(String name, Config config) throws ConfigurationException {
            return new ExplanationFormatter(
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localpagedao"))
            );
        }
    }
}
