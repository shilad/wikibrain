package org.wikapidia.dao.load;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.WikapidiaIterable;
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

/**
 *
 * Idea for changing the flow of parsing:
 * - First load all redirect page id -> page id into memory (TIntIntHashMap).
 * - Fix chaining redirects
 * - Then save.
 * - RedirectSqlDao.update goes away.
 */
public class RedirectLoader {

    private static Language language;
    private static TIntIntHashMap redirectIdsToPageIds;
    private static RawPageSqlDao rawPages;
    private static LocalPageSqlDao localPages;
    private static RedirectSqlDao redirects;

    private static void loadRedirectIdsIntoMemory() throws DaoException{
        RedirectParser redirectParser = new RedirectParser(language);
        redirectIdsToPageIds = new TIntIntHashMap(10, 0.5f, -1, -1);
        WikapidiaIterable<RawPage> redirectPages = rawPages.getAllRedirects(language);
        for(RawPage p : redirectPages){
           Title pTitle = new Title(redirectParser.getRedirect(p.getBody()).getCanonicalTitle(), LanguageInfo.getByLanguage(language));
           redirectIdsToPageIds.put(p.getPageId(),
                    localPages.getIdByTitle(pTitle.getCanonicalTitle(), language, pTitle.getNamespace()));
        }
    }

    private static int resolveRedirect(int src){
        int dest = redirectIdsToPageIds.get(src);
        for(int i = 0; i<4; i++){
            if (redirectIdsToPageIds.get(dest) == -1)
                return dest;
            dest = redirectIdsToPageIds.get(dest);
        }
        return -1;
    }

    private static void resolveRedirectsInMemory(){
        for (int src : redirectIdsToPageIds.keys()) {
            redirectIdsToPageIds.put(src, resolveRedirect(src));
        }
    }

    private static void loadRedirectsIntoDatabase() throws DaoException{
        for(int src : redirectIdsToPageIds.keys()){
            redirects.save(language, src, redirectIdsToPageIds.get(src));
        }
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
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoaderMain", options);
            return;
        }
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        redirects = conf.get(RedirectSqlDao.class);
        localPages = conf.get(LocalPageSqlDao.class);
        rawPages = conf.get(RawPageSqlDao.class);
        language = conf.get(Language.class);


        if (cmd.hasOption("t")){
            redirects.beginLoad();
            loadRedirectIdsIntoMemory();
            resolveRedirectsInMemory();
            loadRedirectsIntoDatabase();
        }

        if (cmd.hasOption("i")){
            redirects.endLoad();
        }
    }

}
