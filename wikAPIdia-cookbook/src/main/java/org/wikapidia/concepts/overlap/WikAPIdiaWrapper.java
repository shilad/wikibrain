package org.wikapidia.concepts.overlap;

import org.apache.commons.collections.IteratorUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalPage;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a wrapper I wrote around the WikAPIdia API for my intro
 * Java course.
 *
 * The design strives to be easy for intro students, so it is not always
 * perfect Java code.
 *
 * @author Shilad Sen
 */
public class WikAPIdiaWrapper {
    private int CONCEPT_ALGORITHM_ID = 1;

    private final Env env;
    private LocalPageDao lpDao;
    private LocalLinkDao llDao;
    private UniversalPageDao upDao;

    public WikAPIdiaWrapper() throws ConfigurationException {
        this.env = new EnvBuilder().build();
        this.lpDao = env.getConfigurator().get(LocalPageDao.class);
        this.llDao = env.getConfigurator().get(LocalLinkDao.class);
        this.upDao = env.getConfigurator().get(UniversalPageDao.class);
    }

    public List<Language> getLanguages() {
        LanguageSet lset = env.getLanguages();
        return new ArrayList<Language>(lset.getLanguages());
    }

    public int getNumInLinks(LocalPage page) throws DaoException {
        DaoFilter filter = new DaoFilter()
                .setLanguages(page.getLanguage())
                .setDestIds(page.getLocalId());
        return llDao.getCount(filter);
    }

    public List<LocalPage> getLocalPages(Language language) throws DaoException {
        DaoFilter df = new DaoFilter()
                .setLanguages(language)
                .setRedirect(false)
                .setDisambig(false)
                .setNameSpaces(NameSpace.ARTICLE);
        return IteratorUtils.toList(lpDao.get(df).iterator());
    }

    public int getConceptId(LocalPage page) throws DaoException {
        return upDao.getUnivPageId(page, CONCEPT_ALGORITHM_ID);
    }

    public List<LocalPage> getConceptPages(int conceptId) throws DaoException {
        List<LocalPage> results = new ArrayList<LocalPage>();
        UniversalPage up = upDao.getById(conceptId, CONCEPT_ALGORITHM_ID);
        if (up == null) {
            return results;
        }
        for (LocalId lid : up.getLocalEntities()) {
            LocalPage lp = lpDao.getById(lid.getLanguage(), lid.getId());
            if (lp != null) {
                results.add(lp);
            }
        }
        return results;
    }
}
