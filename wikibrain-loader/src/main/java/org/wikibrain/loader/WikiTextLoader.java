package org.wikibrain.loader;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalCategoryMember;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.parser.wiki.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the wiki text associated with articles
 * and populates data stores for links, ills, and categories.
 */
public class WikiTextLoader {

    private static final Logger LOG = LoggerFactory.getLogger(WikiTextLoader.class);

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


        LocalLinkVisitor linkVisitor = new LocalLinkVisitor(llDao, lpDao, metaDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(lpDao, lcmDao, metaDao);
        ParserVisitor illVisitor = new InterLanguageLinkVisitor(illDao, lpDao, metaDao);

        visitors.add(linkVisitor);
        visitors.add(catVisitor);
        visitors.add(illVisitor);

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
            LOG.info("loading links for " + lang);

            final LocalLinkSet linkSet = new LocalLinkSet();

            linkVisitor.setLinkListener(
                    new LocalLinkVisitor.Listener() {
                        public void notify(LocalLink link) { linkSet.addLink(link); }
                    });

            WikiTextDumpParser dumpParser = new WikiTextDumpParser(
                    rpDao, LanguageInfo.getByLanguage(lang), LanguageSet.ALL);
            dumpParser.parse(visitors);

            linkSet.finish();

            List<File> paths = env.getFiles(lang, FileMatcher.LINK_SQL);
            if (paths.size() > 1) {
                throw new IllegalStateException();
            }
            if (paths.size() == 1) {
                SqlLinksLoader sqlLoader = new SqlLinksLoader(llDao, lpDao, metaDao, paths.get(0), linkSet);
                sqlLoader.load();
            }
        }

        illDao.endLoad();
        llDao.endLoad();
        lcmDao.endLoad();
        metaDao.endLoad();

        System.out.println("encountered " + metaDao.getInfo(LocalLink.class).getNumErrors() + " parse errors");

        // Why is this necessary???
        // It seems like things die without it :(
        System.exit(0);
    }
}
