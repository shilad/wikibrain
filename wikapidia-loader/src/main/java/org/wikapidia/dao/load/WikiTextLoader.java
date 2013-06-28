package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.parser.wiki.LocalCategoryVisitor;
import org.wikapidia.parser.wiki.LocalLinkVisitor;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.wiki.WikiTextDumpParser;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */

public class WikiTextLoader {

    private final List<ParserVisitor> visitors;
    private final LanguageSet allowedIlls;
    private final RawPageDao rawPageDao;

    public WikiTextLoader(List<ParserVisitor> visitors, LanguageSet allowedIlls, RawPageDao rawPageDao) {
        this.visitors = visitors;
        this.allowedIlls = allowedIlls;
        this.rawPageDao = rawPageDao;
    }

    private void load(LanguageInfo lang) throws DaoException {
        WikiTextDumpParser dumpParser = new WikiTextDumpParser(rawPageDao, lang, allowedIlls);
        dumpParser.parse(visitors);
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

        final WikiTextLoader loader = new WikiTextLoader(visitors, env.getLanguages(), rpDao);

        if(cmd.hasOption("t")) {
            llDao.beginLoad();
            lcmDao.beginLoad();
        }

        ParallelForEach.loop(env.getLanguages().getLanguages(),
                Runtime.getRuntime().availableProcessors(),
                new Procedure<Language>() {
                    @Override
                    public void call(Language lang) throws Exception {
                        loader.load(LanguageInfo.getByLanguage(lang));
                    }
                });

        if (cmd.hasOption("i")) {
            llDao.endLoad();
            lcmDao.endLoad();
        }
    }


}
