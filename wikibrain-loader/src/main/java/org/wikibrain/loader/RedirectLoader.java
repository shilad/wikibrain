package org.wikibrain.loader;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Idea for changing the flow of parsing:
 * - First load all redirect page id -&gt; page id into memory (TIntIntHashMap).
 * - Fix chaining redirects
 * - Then save.
 * - RedirectSqlDao.update goes away.
 */
public class RedirectLoader {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectLoader.class);
    private final MetaInfoDao metaDao;

    private TIntIntHashMap redirectIdsToPageIds;
    private final RawPageDao rawPages;
    private final LocalPageDao localPages;
    private final RedirectDao redirects;

    public RedirectLoader(RawPageDao rpdao, LocalPageDao lpdao, RedirectDao rdao, MetaInfoDao metaDao) throws DaoException{
        this.rawPages = rpdao;
        this.localPages = lpdao;
        lpdao.setFollowRedirects(false);
        this.redirects = rdao;
        this.metaDao = metaDao;
    }

    public RedirectDao getDao() {
        return redirects;
    }

    private void loadRedirectIdsIntoMemory(Language language) throws DaoException{
        redirectIdsToPageIds = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
        int i = 0;
        LOG.info("Begin loading redirects into memory: ");
        for (RawPage p : rawPages.get(new DaoFilter().setLanguages(language).setRedirect(true))) {
           Title pTitle = new Title(p.getRedirectTitle(), LanguageInfo.getByLanguage(language));
           redirectIdsToPageIds.put(p.getLocalId(),
                    localPages.getIdByTitle(pTitle.getCanonicalTitle(), language, pTitle.getNamespace()));
           if(i%100000==0)
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
            metaDao.incrementRecords(Redirect.class, language);
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
                        .create("d"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();

        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);
        LocalPageDao pageDao = conf.get(LocalPageDao.class);

        RedirectLoader redirectLoader = new RedirectLoader(
                conf.get(RawPageDao.class),
                pageDao,
                conf.get(RedirectDao.class),
                metaDao
        );
        if (cmd.hasOption("d")){
            LOG.info("Clearing data provider: ");
            redirectLoader.getDao().clear();
            metaDao.clear(Redirect.class);
        }

        LOG.info("Begin Load: ");
        redirectLoader.getDao().beginLoad();
        metaDao.beginLoad();

        for(Language l : env.getLanguages()){
            LOG.info("LOADING REDIRECTS FOR " + l);
            redirectLoader.loadRedirectIdsIntoMemory(l);
            redirectLoader.resolveRedirectsInMemory();
            redirectLoader.loadRedirectsIntoDatabase(l);
        }

        redirectLoader.getDao().endLoad();
        metaDao.endLoad();

        LOG.info("triggering page title cache creation...");
        pageDao.setFollowRedirects(true);
        LocalPage page = pageDao.getByTitle(env.getDefaultLanguage(), NameSpace.ARTICLE, "FooBar");
    }

}
