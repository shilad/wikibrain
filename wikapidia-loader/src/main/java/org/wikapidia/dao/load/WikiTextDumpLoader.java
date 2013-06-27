package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.LanguageInfo;
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

public class WikiTextDumpLoader {

    private final List<ParserVisitor> visitors;
    private final List<String> allowedIlls;
    private final RawPageDao rawPageDao;

    public WikiTextDumpLoader(List<ParserVisitor> visitors, List<String> allowedIlls, RawPageDao rawPageDao) {
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

        List<String> languages;
        if (cmd.hasOption("l")){
            languages = Arrays.asList(cmd.getOptionValues('l'));
        } else {
            languages = (List<String>)conf.getConf().get().getAnyRef("Languages");
        }

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

        final WikiTextDumpLoader loader = new WikiTextDumpLoader(visitors, languages, rpDao);

        if(cmd.hasOption("t")) {
            llDao.beginLoad();
            lcmDao.beginLoad();
        }

        ParallelForEach.loop(languages,
                Runtime.getRuntime().availableProcessors(),
                new Procedure<String>() {
                    @Override
                    public void call(String lang) throws Exception {
                        loader.load(LanguageInfo.getByLangCode(lang));
                    }
                });

        if (cmd.hasOption("i")) {
            llDao.endLoad();
            lcmDao.endLoad();
        }
    }


}
