package org.wikibrain.dao.load;

import org.apache.commons.cli.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalCategoryMember;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.parser.wiki.*;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Parses the wiki text associated with articles
 * and populates data stores for links, ills, and categories.
 */
public class WikiTextLoader {

    private static final Logger LOG = Logger.getLogger(WikiTextLoader.class.getName());

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
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

        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        RawPageDao rpDao = conf.get(RawPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        LocalCategoryMemberDao lcmDao = conf.get(LocalCategoryMemberDao.class);
        InterLanguageLinkDao illDao = conf.get(InterLanguageLinkDao.class);

        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);


        ParserVisitor linkVisitor = new LocalLinkVisitor(llDao, lpDao, metaDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(lpDao, lcmDao, metaDao);
        ParserVisitor illVisitor = new InterLanguageLinkVisitor(illDao, lpDao, metaDao);

        visitors.add(linkVisitor);
        visitors.add(catVisitor);
        visitors.add(illVisitor);
        visitors.add(null);             // place holder for map db visitor

        if(cmd.hasOption("d")) {
            llDao.clear();
            lcmDao.clear();
            illDao.clear();
            metaDao.clear(LocalLink.class);
            metaDao.clear(LocalCategoryMember.class);
            metaDao.clear(InterLanguageLink.class);
        }
        illDao.beginLoad();
        llDao.beginLoad();
        lcmDao.beginLoad();
        metaDao.beginLoad();

        for (Language lang : env.getLanguages().getLanguages()) {
            DB db = DBMaker
                    .newTempFileDB()
                    .mmapFileEnable()
                    .transactionDisable()
                    .asyncWriteEnable()
                    .asyncWriteFlushDelay(100)
                    .make();

            DB.BTreeSetMaker setMaker = db.createTreeSet("linkHashes");
            MapDbLinkListener linkListener = new MapDbLinkListener(setMaker);

            WikiTextDumpParser dumpParser = new WikiTextDumpParser(
                    rpDao, LanguageInfo.getByLanguage(lang), LanguageSet.ALL);
            dumpParser.parse(visitors);

            db.close();
        }

        illDao.endLoad();
        llDao.endLoad();
        lcmDao.endLoad();
        metaDao.endLoad();

        LOG.info("optimizing database.");
        conf.get(WpDataSource.class).optimize();

        System.out.println("encountered " + metaDao.getInfo(LocalLink.class).getNumErrors() + " parse errors");

        // Why is this necessary???
        // It seems like things die without it :(
        System.exit(0);
    }
}
