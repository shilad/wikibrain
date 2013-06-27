package org.wikapidia.dao.load;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.SqlDaoIterable;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.dao.sql.RawPageSqlDao;
import org.wikapidia.core.dao.sql.RedirectSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.parser.wiki.RedirectParser;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Idea for changing the flow of parsing:
 * - First load all redirect page id -> page id into memory (TIntIntHashMap).
 * - Fix chaining redirects
 * - Then save.
 * - RedirectSqlDao.update goes away.
 */
public class RedirectLoader {

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
        System.out.println("Begin Load: ");
    }

    private void endLoad() throws DaoException {
        redirects.endLoad();
        System.out.println("End Load.");
    }

    private void loadRedirectIdsIntoMemory(Language language) throws DaoException{
        RedirectParser redirectParser = new RedirectParser(language);
        redirectIdsToPageIds = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
        SqlDaoIterable<RawPage> redirectPages = rawPages.getAllRedirects(language);
        int i = 0;
        System.out.println("Begin loading redirects into memory: ");
        for(RawPage p : redirectPages){
           Title pTitle = new Title(p.getRedirectTitle(), LanguageInfo.getByLanguage(language));
           redirectIdsToPageIds.put(p.getPageId(),
                    localPages.getIdByTitle(pTitle.getCanonicalTitle(), language, pTitle.getNamespace()));
           if(i%1000==0)
               System.out.println("loading redirect # " + i);
            i++;
        }
        System.out.println("End loading redirects into memory.");
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
            if(i%100==0)
                System.out.println("resolving redirect # " + i);
            i++;
        }
    }

    private void loadRedirectsIntoDatabase(Language language) throws DaoException{
        int i = 0;
        System.out.println("Begin loading redirects into database: ");
        for(int src : redirectIdsToPageIds.keys()){
            if(i%100==0)
                System.out.println("loaded " + i + " into database.");
            redirects.save(language, src, redirectIdsToPageIds.get(src));
            i++;
        }
        System.out.println("End loading redirects into database.");
    }

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("conf")
                        .withDescription("configuration file")
                        .create("c"));
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
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process")
                        .create("l"));
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        File pathConf = cmd.hasOption('c') ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        DataSource dataSource = conf.get(DataSource.class);
        RedirectLoader redirectLoader = new RedirectLoader(dataSource);
        if (cmd.hasOption("t")){
            redirectLoader.beginLoad();
        }

        List<String> languages;
        if (cmd.hasOption("l")) {
            languages = Arrays.asList(cmd.getOptionValues("l"));
        } else {
            languages = (List<String>)conf.getConf().get().getAnyRef("Languages");
        }
        for(String l : languages){
            Language lang = Language.getByLangCode(l);
            redirectLoader.loadRedirectIdsIntoMemory(lang);
            redirectLoader.resolveRedirectsInMemory();
            redirectLoader.loadRedirectsIntoDatabase(lang);
        }

        if (cmd.hasOption("i")){
            redirectLoader.endLoad();
        }
    }

}
