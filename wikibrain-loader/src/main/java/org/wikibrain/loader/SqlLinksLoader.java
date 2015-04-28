package org.wikibrain.loader;

import gnu.trove.set.TIntSet;
import org.apache.commons.lang.StringUtils;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.parser.sql.MySqlDumpParser;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads links that are in the SQL dump but not the parsed wiki text.
 */
public class SqlLinksLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SqlLinksLoader.class);

    private final AtomicInteger counter = new AtomicInteger();
    private final File sqlDump;
    private final Language language;
    private TIntSet validIds;

    private final LocalLinkDao dao;
    private final LocalPageDao pageDao;
    private final LocalLinkSet existing;
    private final MetaInfoDao metaDao;

    private AtomicLong totalLinks = new AtomicLong();
    private AtomicLong interestingLinks = new AtomicLong();
    private AtomicLong newLinks = new AtomicLong();


    public SqlLinksLoader(LocalLinkDao dao, LocalPageDao pageDao, MetaInfoDao metaDao, File file, LocalLinkSet existing) throws DaoException {
        this.dao = dao;
        this.metaDao = metaDao;
        this.pageDao = pageDao;
        this.sqlDump = file;
        this.language = FileMatcher.LINK_SQL.getLanguage(file.getAbsolutePath());
        int n = dao.getCount(new DaoFilter().setLanguages(language));
        n = Math.max(10000, n);
        n *= 2 * 3; // guess that there will be twice as many links as there are now, to be safe, array size should be 3 times as big.
        LOG.info("guessing at size of array at " + n);
        this.existing = existing;
    }

    public void load() throws DaoException {
        totalLinks.set(0);
        newLinks.set(0);
        interestingLinks.set(0);

        ParallelForEach.iterate(
                new MySqlDumpParser().parse(sqlDump).iterator(),
                WpThreadUtils.getMaxThreads(),
                1000,
                new Procedure<Object[]>() {
                    @Override
                    public void call(Object[] row) throws Exception {
                        processOneLink(row);
                    }
                },
                1000000
        );
    }

    private void processOneLink(Object[] row) throws DaoException {
        if (totalLinks.incrementAndGet() % 100000 == 0) {
            LOG.info("Processed link " + totalLinks + ", found " + interestingLinks + " interesting and " + newLinks + " new");
        }

        Integer srcPageId = (Integer) row[0];
        Integer destNamespace = (Integer) row[1];
        String destTitle = (String) row[2];
        NameSpace ns = NameSpace.getNameSpaceByValue(destNamespace);

        // TODO: make this configurable
        if (ns == null || (ns != NameSpace.ARTICLE && ns != NameSpace.CATEGORY)) {
            return;
        }
        if (srcPageId < 0 || StringUtils.isEmpty(destTitle)) {
            return;
        }

        interestingLinks.incrementAndGet();
        Title title = new Title(destTitle, LanguageInfo.getByLanguage(language));
        int destId = pageDao.getIdByTitle(title.getTitleStringWithoutNamespace(), language, ns);
        if (destId < 0) {
            // Handle red link
        } else if (validIds != null && (!validIds.contains(srcPageId) || !validIds.contains(destId))) {
            // Skip
        } else {

            LocalLink ll = new LocalLink(language, "", srcPageId, destId,
                    true, -1, false, LocalLink.LocationType.NONE);
            if (!existing.contains(ll)) {
                newLinks.incrementAndGet();
                dao.save(ll);
            }
        }
    }

    public void setValidIds(TIntSet validIds) {
        this.validIds = validIds;
    }
}
