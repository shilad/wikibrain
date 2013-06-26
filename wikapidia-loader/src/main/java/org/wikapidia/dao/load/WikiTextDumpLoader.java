package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
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

    public WikiTextDumpLoader(List<ParserVisitor> visitors, List<String> allowedIlls) {
        this.visitors = visitors;
        this.allowedIlls = allowedIlls;
    }


    public static LanguageInfo getLanguageInfo(File file) {
        int i = file.getName().indexOf("wiki");
        if (i < 0) {
            throw new IllegalArgumentException("invalid filename. Expected prefix, for example 'enwiki-...'");
        }
        String langCode = file.getName().substring(0, i);
        return LanguageInfo.getByLangCode(langCode);
    }

    private void load(File file) {
        int i = file.getName().indexOf("wiki");
        if (i < 0) {
            throw new IllegalArgumentException("invalid filename. Expected prefix, for example 'enwiki-...'");
        }
        String langCode = file.getName().substring(0, i);
        langCode = langCode.replace('_', '-');
        LanguageInfo lang = LanguageInfo.getByLangCode(langCode);
        WikiTextDumpParser dumpParser = new WikiTextDumpParser(file, lang, allowedIlls);
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
                        .withLongOpt("ills")
                        .withDescription("ills allowed")
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
        if (cmd.getArgList().isEmpty()) {
            System.err.println("No input files specified.");
            new HelpFormatter().printHelp("WikiTextDumpLoader", options);
            return;
        }
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        List<String> allowedIlls = null;
        if (cmd.hasOption("l")){
            allowedIlls = Arrays.asList(cmd.getOptionValue('l').split(","));
        }

        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        LocalCategoryMemberDao lcmDao = conf.get(LocalCategoryMemberDao.class);

        ParserVisitor linkVisitor = new LocalLinkVisitor(llDao, lpDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(lpDao, lcmDao);
        //TODO: ill visitor

        visitors.add(linkVisitor);
        visitors.add(catVisitor);

        final WikiTextDumpLoader loader = new WikiTextDumpLoader(visitors, allowedIlls);

        if(cmd.hasOption("t")) {
            llDao.beginLoad();
            lcmDao.beginLoad();
        }

        ParallelForEach.loop(cmd.getArgList(),
                Runtime.getRuntime().availableProcessors(),
                new Procedure<String>() {
                    @Override
                    public void call(String path) throws Exception {
                        loader.load(new File(path));
                    }
                });

        if (cmd.hasOption("i")) {
            llDao.endLoad();
            lcmDao.endLoad();
        }
    }


}
