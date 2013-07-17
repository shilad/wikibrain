package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.parser.wiki.*;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses the wiki text associated with articles
 * and populates data stores for links, ills, and categories.
 */
public class WikiTextLoader {

    /**
     * The maximum number of threads to use for a single language edition.
     */
    public static int maxThreadsPerLang = 8;


    private final List<ParserVisitor> visitors;
    private final LanguageSet allowedIlls;
    private final RawPageDao rawPageDao;
    private final AtomicInteger availableThreads;

    public WikiTextLoader(List<ParserVisitor> visitors, LanguageSet allowedIlls, RawPageDao rawPageDao, int maxThreads) {
        this.visitors = visitors;
        this.allowedIlls = allowedIlls;
        this.rawPageDao = rawPageDao;
        this.availableThreads = new AtomicInteger(maxThreads);
    }

    public RawPageDao getDao() {
        return rawPageDao;
    }

    private void load(LanguageInfo lang) throws DaoException {
        int numLanguageThreads;
        synchronized (availableThreads) {
            numLanguageThreads = Math.min(availableThreads.get(), maxThreadsPerLang);
            availableThreads.getAndAdd(-numLanguageThreads);
        }
        try {
            WikiTextDumpParser dumpParser = new WikiTextDumpParser(rawPageDao, lang, allowedIlls);
            dumpParser.setMaxThreads(numLanguageThreads);
            dumpParser.parse(visitors);
        } finally {
            synchronized (availableThreads) {
                availableThreads.getAndAdd(numLanguageThreads);
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
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


        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        RawPageDao rpDao = conf.get(RawPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        LocalCategoryMemberDao lcmDao = conf.get(LocalCategoryMemberDao.class);

        ParserVisitor linkVisitor = new LocalLinkVisitor(llDao, lpDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(lpDao, lcmDao);
        //TODO: ill visitor

        visitors.add(linkVisitor);
        visitors.add(catVisitor);

        final WikiTextLoader loader = new WikiTextLoader(visitors, env.getLanguages(), rpDao, env.getMaxThreads());

        if(cmd.hasOption("d")) {
            llDao.clear();
            lcmDao.clear();
        }
        llDao.beginLoad();
        lcmDao.beginLoad();

        ParallelForEach.loop(env.getLanguages().getLanguages(),
                Math.max(1, env.getLanguages().size() / maxThreadsPerLang),
                new Procedure<Language>() {
                    @Override
                    public void call(Language lang) throws Exception {
                        loader.load(LanguageInfo.getByLanguage(lang));
                    }
                });

        llDao.endLoad();
        lcmDao.endLoad();
    }


}
