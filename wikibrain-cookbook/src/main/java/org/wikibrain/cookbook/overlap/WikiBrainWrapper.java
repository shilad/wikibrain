package org.wikibrain.cookbook.overlap;

import org.apache.commons.collections.IteratorUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.Main;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a wrapper I wrote around the WikiBrain API for Shilad's intro Java course.
 *
 * The design strives to be understandable to intro students, so parts of it may seem
 * awkward to experienced Java programmers.
 *
 * @author Shilad Sen
 */
public class WikiBrainWrapper {
    private int CONCEPT_ALGORITHM_ID = 1;

    public static String BASE_DIR = ".";

    private final Env env;
    private LocalPageDao lpDao;
    private LocalLinkDao llDao;
    private UniversalPageDao upDao;

    /**
     * Creates a new wrapper object with default configuration settings.
     *
     * @throws org.wikibrain.conf.ConfigurationException
     */
    public WikiBrainWrapper(String baseDir) throws ConfigurationException {
        env = new EnvBuilder().setBaseDir(new File(baseDir)).build();
        this.lpDao = env.getConfigurator().get(LocalPageDao.class);
        this.llDao = env.getConfigurator().get(LocalLinkDao.class);
        this.upDao = env.getConfigurator().get(UniversalPageDao.class);
    }

    /**
     * @return The list of installed languages.
     */
    public List<Language> getLanguages() {
        LanguageSet lset = env.getLanguages();
        return new ArrayList<Language>(lset.getLanguages());
    }

    /**
     * Returns the number of WikiLinks to a particular page.
     * @param page
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    public int getNumInLinks(LocalPage page) throws DaoException {
        DaoFilter filter = new DaoFilter()
                .setLanguages(page.getLanguage())
                .setDestIds(page.getLocalId());
        return llDao.getCount(filter);
    }

    /**
     * Returns a list of ALL the local pages in a particular language.
     * @param language
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    public List<LocalPage> getLocalPages(Language language) throws DaoException {
        DaoFilter df = new DaoFilter()
                .setLanguages(language)
                .setRedirect(false)
                .setDisambig(false)
                .setNameSpaces(NameSpace.ARTICLE);
        return IteratorUtils.toList(lpDao.get(df).iterator());
    }

    /**
     * Returns a list of the pages that represent the same concept in other languages.
     * @param page
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    public List<LocalPage> getInOtherLanguages(LocalPage page) throws DaoException {
        int conceptId = upDao.getUnivPageId(page);
        List<LocalPage> results = new ArrayList<LocalPage>();
        UniversalPage up = upDao.getById(conceptId);
        if (up == null) {
            return results;
        }
        for (LocalId lid : up.getLocalEntities()) {
            if (!lid.equals(page.toLocalId())) {
                LocalPage lp = lpDao.getById(lid.getLanguage(), lid.getId());
                if (lp != null) {
                    results.add(lp);
                }
            }
        }
        return results;
    }

    /**
     * Load a set of languages into the h2 database.
     * THIS MUST BE CALLED BEFORE AN INSTANCE OF WIKIPADIA WRAPPER IS CREATED!
     * @param langCodes comma separated list of langcodes - ie "simple,la"
     */
    public static void loadLanguages(String langCodes) throws IOException, InterruptedException, ClassNotFoundException, ConfigurationException, SQLException, DaoException {
        Main.main(new String[]{"-l", langCodes});
    }
}
