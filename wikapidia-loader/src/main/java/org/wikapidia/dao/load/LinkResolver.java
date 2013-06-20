package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.io.File;


/**
 */
public class LinkResolver {
    private final Language language;
    private final LocalLinkSqlDao linkDao;
    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;

    public LinkResolver(Language language, LocalLinkSqlDao linkDao, LocalPageDao pageDao, LocalCategoryMemberDao catMemDao){
        this.language = language;
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.catMemDao = catMemDao;
    }


    private void resolveAllLinks() throws DaoException {
        SqlDaoIterable<LocalLink> localLinks = linkDao.getLinks(language, true);
        for (LocalLink link : localLinks){
            if (isCategory(link)) {
                resolveCategoryMember(link);
            }
            else {
                resolveLink(link);
            }
        }

    }

    private void resolveLink(LocalLink link) throws DaoException {
        String linkText = link.getAnchorText().split("|")[0];
        if (isLinkToCategory(linkText)){
            linkText = linkText.substring(1,linkText.length());
        }
        Title linkTitle = new Title(linkText, LanguageInfo.getByLanguage(language));
        int destId = pageDao.getIdByTitle(linkTitle.getCanonicalTitle(), language, linkTitle.getNamespace());
        LocalLink newLink = new LocalLink(
                link.getLanguage(),
                link.getAnchorText(),
                link.getSourceId(),
                destId,
                link.isOutlink(),
                link.getLocation(),
                link.isParseable(),
                link.getLocType()
        );
        linkDao.update(newLink);
    }

    private void resolveCategoryMember(LocalLink link) throws DaoException {
        linkDao.remove(link);
        String catText = link.getAnchorText().split("|")[0];
        Title catTitle = new Title(catText, LanguageInfo.getByLanguage(language));
        assert (catTitle.getNamespace().equals(NameSpace.CATEGORY));
        int catId = pageDao.getIdByTitle(catTitle.getCanonicalTitle(), language, NameSpace.CATEGORY);
        LocalCategoryMember catMem = new LocalCategoryMember(catId, link.getSourceId(), language);
        catMemDao.save(catMem);
    }

    private boolean isCategory(LocalLink link){
        for(String categoryName : LanguageInfo.getByLanguage(link.getLanguage()).getCategoryNames()){
            if(link.getAnchorText().substring(categoryName.length() + 1).toLowerCase().equals(categoryName + ":")){
                return true;
            }
        }
        return false;
    }

    private boolean isLinkToCategory(String link){
        for(String categoryName : LanguageInfo.getByLanguage(language).getCategoryNames()){
            if(link.substring(categoryName.length() + 2).toLowerCase().equals(":" + categoryName + ":")){
                return true;
            }
        }
        return false;
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
                        .hasArg()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create('t'));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("create-indexes")
                        .withDescription("create all indexes after loading")
                        .create("i"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("language")
                        .withDescription("selects the language, default en")
                        .create("l"));
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options,args);
        } catch (ParseException e){
            System.err.println ("Invalid option usage: " +e.getMessage());
            new org.apache.commons.cli.HelpFormatter().printHelp("DumpLoader", options);
            return;
        }
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalCategoryMemberDao catMemDao = conf.get(LocalCategoryMemberDao.class);

        Language lang = cmd.hasOption("l") ? Language.getByLangCode(cmd.getOptionValue("l")): Language.getByLangCode("en");

        final LinkResolver resolver = new LinkResolver(lang, llDao, lpDao, catMemDao);

        if (cmd.hasOption("t")){
            catMemDao.beginLoad();
        }

        resolver.resolveAllLinks();

        if (cmd.hasOption("i")){
            llDao.endLoad();
            catMemDao.endLoad();
        }



    }


}
