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

import java.io.File;


/**
 */
public class LinkResolver {
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;

     public LinkResolver(LocalLinkDao linkDao, LocalPageDao pageDao, LocalCategoryMemberDao catMemDao){
         this.linkDao = linkDao;
         this.pageDao = pageDao;
         this.catMemDao = catMemDao;
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
         LocalCategoryMemberDao lcmDao = conf.get(LocalCategoryMemberDao.class);

         final LinkResolver resolver = new LinkResolver(llDao, lpDao,lcmDao);

         if (cmd.hasOption("t")){
             llDao.beginLoad();
             lcmDao.beginLoad();
         }

         //Stuff happens

         if (cmd.hasOption("i")){
             llDao.endLoad();
             lcmDao.endLoad();
         }



     }


}
