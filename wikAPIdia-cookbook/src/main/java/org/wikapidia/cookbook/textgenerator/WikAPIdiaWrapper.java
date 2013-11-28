package org.wikapidia.cookbook.textgenerator;

import org.apache.commons.io.FilenameUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.sql.SimpleSqlDaoIterable;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.*;
import org.wikapidia.parser.wiki.MarkupStripper;

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

    private static final int CONCEPT_ALGORITHM_ID = 1;

    private final Env env;
    private RawPageDao rpDao;
    private LocalPageDao lpDao;
    private UniversalPageDao upDao;
    private LocalCategoryMemberDao cmDao;

    /**
     * Creates a new wrapper object with default configuration settings.
     *
     * baseDir should be the parent "wikAPIdia" directory containing the "db" directory.
     * You must have read / write permissions in this directory.
     */
    public WikAPIdiaWrapper(String baseDir) {
        try {
            File dbDir = new File(baseDir);
            String basename = FilenameUtils.getBaseName(dbDir.getAbsolutePath());
            if (!basename.equals("db")) {
                dbDir = new File(dbDir, "db");
            }
            System.out.println("Using wikAPIdia path: " + dbDir.getParentFile().getAbsolutePath());
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
            this.upDao = env.getConfigurator().get(UniversalPageDao.class);
            this.lpDao = env.getConfigurator().get(LocalPageDao.class);
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
     * Returns text from the first n page texts in a particular language
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
                String cleaned = MarkupStripper.stripEverything(rp.getBody());
                String buffer = "";
                for (String line : cleaned.split("\n+")) {
                    if (isInterestingParagraph(line)) {
                        if (!buffer.isEmpty()) buffer += "\n\n";
                        buffer += line;
                    }
                }
                buffer = buffer.replaceAll("\n+", "\n\n");
                texts.add(buffer);
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
     * @param text
     * @return true if the paragraph contains interesting text.
     */
    private boolean isInterestingParagraph(String text) {
        if (text.split(" +").length < 10) {
            return false;
        }
        for (char c : "-{}!?*[]&%$#@()".toCharArray()) {
            if (text.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
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
}
