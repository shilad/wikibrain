package org.wikapidia.cookbook.wikiwalker;

import gnu.trove.map.hash.TLongByteHashMap;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shilad wrote this wrapper around the WikAPIdia API for COMP 124.
 *
 * The design strives to be understandable to intro students, so parts of it may seem
 * awkward to experienced Java programmers.
 *
 * @author Shilad Sen
 */
public class WikAPIdiaWrapper {

    private final Env env;
    private final RawPageDao rpDao;
    private final LocalPageDao lpDao;
    private final LocalLinkDao llDao;

    /**
     * Local page ids that are "interesting."
     * This tries to exclude things like lists and categories.
     */
    private final TLongByteHashMap idsAreInteresting = new TLongByteHashMap();
    private static final byte INTERESTING = 'y';
    private static final byte NOT_INTERESTING = 'n';

    /**
     * Creates a new wrapper object with default configuration settings.
     *
     * baseDir should be the parent "wikAPIdia" directory containing the "db" directory.
     * You must have read / write permissions in this directory.
     */
    public WikAPIdiaWrapper(String baseDir) {
        try {
            File wpDir = new File(baseDir);

            /* These lines make sure student directories are set up correctly
            String basename = FilenameUtils.getBaseName(wpDir.getAbsolutePath());
            if (!basename.equals("wikAPIdia")) {
                System.err.println("baseDir should end in 'wikAPIdia', but found '" + basename + "'");
                System.exit(1);
            }
            File dbDir = new File(wpDir, "db");
            System.out.println("Using wikAPIdia path: " + dbDir.getParentFile().getAbsolutePath());
            if (!dbDir.isDirectory() || !new File(dbDir, "h2.h2.db").isFile()) {
                System.err.println(
                        "Database directory " + dbDir + " does not exist or is missing h2.h2.db file." +
                                "Have you downloaded and extracted the database?" +
                                "Are you running the program from the right directory?"
                );
                System.exit(1);
            } */

            env = new EnvBuilder()
                    .setBaseDir(wpDir.getAbsolutePath())
                    .build();
            this.lpDao = env.getConfigurator().get(LocalPageDao.class);
            this.llDao = env.getConfigurator().get(LocalLinkDao.class);
            this.rpDao = env.getConfigurator().get(RawPageDao.class);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        // Hack to make students' life easier
        synchronized (WikAPIdiaWrapper.class) {
            if (SINGLETON != null) {
                throw new IllegalArgumentException("Only one wikapidia wrapper can be created per program");
            }
            SINGLETON = this;
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
     * Returns a local page with a particular id.
     */
    public LocalPage getLocalPageById(Language language, int id) {
        try {
            return lpDao.getById(language, id);
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

    /**
     * Returns pages that have this category.
     */
    public List<Integer> getLinkedIds(Language lang, int id) {
        try {
            DaoFilter filter = new DaoFilter()
                    .setLanguages(lang)
                    .setNameSpaces(NameSpace.ARTICLE)
                    .setSourceIds(id)
                    .setRedirect(false);
            List<Integer> pages = new ArrayList<Integer>();
            for (LocalLink link : llDao.get(filter)) {
                pages.add(link.getDestId());
            }
            return pages;
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the number of links that appear on a particular wiki page
     * @param lang
     * @param id
     * @return
     */
    public int getNumOutLinks(Language lang, int id) {
        try {
            DaoFilter filter = new DaoFilter()
                    .setLanguages(lang)
                    .setNameSpaces(NameSpace.ARTICLE)
                    .setSourceIds(id)
                    .setRedirect(false);
            return llDao.getCount(filter);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean isInteresting(Language lang, int id) {
        long packed = new LocalId(lang, id).toLong();
        if (!idsAreInteresting.containsKey(packed)) {
            setInteresting(lang, id, getNumOutLinks(lang, id) <= 30);
        }
        return idsAreInteresting.get(packed) == INTERESTING;
    }

    public synchronized void setInteresting(Language lang, int id, boolean interesting) {
        long packed = new LocalId(lang, id).toLong();
        idsAreInteresting.put(packed, interesting ? INTERESTING : NOT_INTERESTING);
    }

    // Hack to make students' life easier
    private static WikAPIdiaWrapper SINGLETON = null;
    public static WikAPIdiaWrapper getInstance() {
        return SINGLETON;
    }
}
