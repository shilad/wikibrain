package org.wikibrain.cookbook.entities;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FilenameUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.SimpleSqlDaoIterable;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shilad wrote this wrapper around the WikiBrain API for COMP 124.
 *
 * The design strives to be understandable to intro students, so parts of it may seem
 * awkward to experienced Java programmers.
 *
 * @author Shilad Sen
 */
public class WikiBrainWrapper {

    private static final int CONCEPT_ALGORITHM_ID = 1;

    private final Env env;
    private RawPageDao rpDao;
    private LocalPageDao lpDao;
    private LocalLinkDao llDao;
    private UniversalPageDao upDao;
    private LocalCategoryMemberDao cmDao;

    /**
     * Creates a new wrapper object with default configuration settings.
     *
     * baseDir should be the parent "wikibrain" directory containing the "db" directory.
     * You must have read / write permissions in this directory.
     */
    public WikiBrainWrapper(String baseDir) {
        try {
            File dbDir = new File(baseDir);
            String basename = FilenameUtils.getBaseName(dbDir.getAbsolutePath());
            if (!basename.equals("db")) {
                dbDir = new File(dbDir, "db");
            }
            System.out.println("Using wikibrain path: " + dbDir.getParentFile().getAbsolutePath());
            if (!dbDir.isDirectory() || !new File(dbDir, "h2.h2.db").isFile()) {
                System.err.println(
                        "Database directory " + dbDir + " does not exist or is missing h2.h2.db file." +
                                "Have you downloaded and extracted the database?" +
                                "Are you running the program from the right directory?"
                );
                System.exit(1);
            }
            env = new EnvBuilder()
                    .setBaseDir(dbDir.getParentFile().getAbsolutePath())
                    .build();
            this.rpDao = env.getConfigurator().get(RawPageDao.class);
            this.lpDao = env.getConfigurator().get(LocalPageDao.class);
            this.llDao = env.getConfigurator().get(LocalLinkDao.class);
            this.upDao = env.getConfigurator().get(UniversalPageDao.class);
            this.cmDao = env.getConfigurator().get(LocalCategoryMemberDao.class);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The list of installed languages.
     */
    public List<Language> getLanguages() {
        LanguageSet lset = env.getLanguages();
        return new ArrayList<Language>(lset.getLanguages());
    }

    /**
     * Returns a local page with a particular title.
     */
    public LocalPage getLocalPageByTitle(Language language, String title) {
        try {
            return lpDao.getByTitle(new Title(title, language), NameSpace.ARTICLE);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the number of WikiLinks to a particular page.
     * @param page
     * @return
     */
    public int getNumInLinks(LocalPage page) {
        DaoFilter filter = new DaoFilter()
                .setLanguages(page.getLanguage())
                .setDestIds(page.getLocalId());
        try {
            return llDao.getCount(filter);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of ALL the local pages in a particular language.
     * @param language
     * @return
     */
    public List<LocalPage> getLocalPages(Language language) {
        DaoFilter df = new DaoFilter()
                .setLanguages(language)
                .setRedirect(false)
                .setDisambig(false)
                .setNameSpaces(NameSpace.ARTICLE);
        try {
            return IteratorUtils.toList(lpDao.get(df).iterator());
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the first n page texts in a particular language
     * @param language
     * @param n
     * @return
     */
    public List<String> getPageTexts(Language language, int n) {
        DaoFilter df = new DaoFilter()
                .setLanguages(language)
                .setRedirect(false)
                .setDisambig(false)
                .setLimit(n)
                .setNameSpaces(NameSpace.ARTICLE);
        try {
            List<String> texts = new ArrayList<String>();
            SimpleSqlDaoIterable<RawPage> iterable = (SimpleSqlDaoIterable<RawPage>) rpDao.get(df);
            for (RawPage rp : iterable) {
                texts.add(rp.getBody());
                if (texts.size() >= n) {
                    break;
                }
            }
            iterable.close();
            return texts;
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of the pages that represent the same concept in other languages.
     *
     * @param page
     * @return All pages that represent the same concept, INCLUDING the page passed as an argument.
     */
    public List<LocalPage> getInOtherLanguages(LocalPage page) {
        try {
            int conceptId = upDao.getUnivPageId(page, CONCEPT_ALGORITHM_ID);
            List<LocalPage> results = new ArrayList<LocalPage>();
            UniversalPage up = upDao.getById(conceptId, CONCEPT_ALGORITHM_ID);
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
            results.add(page);
            return results;
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the wiki markup of a page with some text.
     *
     * @param page
     * @return
     */
    public String getPageText(LocalPage page) {
        try {
            return rpDao.getById(page.getLanguage(), page.getLocalId()).getBody();
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }
}
