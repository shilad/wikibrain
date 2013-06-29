package org.wikapidia.dao.load;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.SqlDaoIterable;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.dao.sql.RawPageSqlDao;
import org.wikapidia.core.dao.sql.RedirectSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.parser.wiki.RedirectParser;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * Idea for changing the flow of parsing:
 * - First load all redirect page id -> page id into memory (TIntIntHashMap).
 * - Fix chaining redirects
 * - Then save.
 * - RedirectSqlDao.update goes away.
 */
public class RedirectLoader {
    private static final Logger LOG = Logger.getLogger(RedirectLoader.class.getName());

    private TIntIntHashMap redirectIdsToPageIds;
    private final RawPageSqlDao rawPages;
    private final LocalPageSqlDao localPages;
    private final RedirectSqlDao redirects;

    public RedirectLoader(DataSource ds) throws DaoException{
        this.rawPages = new RawPageSqlDao(ds);
        this.localPages = new LocalPageSqlDao(ds,false);
        this.redirects = new RedirectSqlDao(ds);
    }

    private void beginLoad() throws DaoException {
        redirects.beginLoad();
        LOG.info("Begin Load: ");
    }

    private void endLoad() throws DaoException {
        redirects.endLoad();
        LOG.info("End Load.");
    }

    private void loadRedirectIdsIntoMemory(Language language) throws DaoException{
        redirectIdsToPageIds = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
        int i = 0;
        LOG.info("Begin loading redirects into memory: ");
        for (RawPage p : rawPages.getAllRedirects(language)) {
           Title pTitle = new Title(p.getRedirectTitle(), LanguageInfo.getByLanguage(language));
           redirectIdsToPageIds.put(p.getPageId(),
                    localPages.getIdByTitle(pTitle.getCanonicalTitle(), language, pTitle.getNamespace()));
           if(i%10000==0)
               LOG.info("loading redirect # " + i);
            i++;
        }
        LOG.info("End loading redirects into memory.");
    }

    private int resolveRedirect(int src){
        int dest = redirectIdsToPageIds.get(src);
        for(int i = 0; i<4; i++){
            if (redirectIdsToPageIds.get(dest) == -1)
                return dest;
            dest = redirectIdsToPageIds.get(dest);
        }
        return -1;
    }

    private void resolveRedirectsInMemory(){
        int i = 0;
        for (int src : redirectIdsToPageIds.keys()) {
            redirectIdsToPageIds.put(src, resolveRedirect(src));
            if(i%10000==0)
                LOG.info("resolving redirect # " + i);
            i++;
        }
    }

    private void loadRedirectsIntoDatabase(Language language) throws DaoException{
        int i = 0;
        LOG.info("Begin loading redirects into database: ");
        for(int src : redirectIdsToPageIds.keys()){
            if(i%10000==0)
                LOG.info("loaded " + i + " into database.");
            redirects.save(language, src, redirectIdsToPageIds.get(src));
            i++;
        }
        LOG.info("End loading redirects into database.");
    }

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("t"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("create-indexes")
                        .withDescription("create all indexes after loading")
                        .create("i"));
        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();

        DataSource dataSource = conf.get(DataSource.class);
        RedirectLoader redirectLoader = new RedirectLoader(dataSource);
        if (cmd.hasOption("t")){
            redirectLoader.beginLoad();
        }

        for(Language l : env.getLanguages()){
            LOG.info("LOADING REDIRECTS FOR " + l);
            redirectLoader.loadRedirectIdsIntoMemory(l);
            redirectLoader.resolveRedirectsInMemory();
            redirectLoader.loadRedirectsIntoDatabase(l);
        }

        if (cmd.hasOption("i")){
            redirectLoader.endLoad();
        }
    }

}
