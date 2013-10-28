package org.wikapidia.sr.cookbook;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.ExplanationFormatter;

/**
 * @author Matt Lesicko
 */
public class MostSimilarDemo {
    private static void localPrintResult(SRResult result, Language language,LocalPageDao localPageDao, ExplanationFormatter expf) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            LocalPage namepage = localPageDao.getById(language, result.getId());
            if (namepage!=null){
                System.out.println(namepage.getTitle().getCanonicalTitle());
            }
            System.out.println("Similarity score: "+result.getScore());
            int explanationsSeen = 0;
            for (Explanation explanation : result.getExplanations()){
                System.out.println(expf.formatExplanation(explanation));
                if (++explanationsSeen>5){
                    break;
                }
            }
        }

    }




    public static void main(String[] args) throws Exception{
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));
        options.addOption(
                new DefaultOptionBuilder()
                    .hasArgs()
                    .withLongOpt("phrase")
                    .withDescription("phrase")
                    .create("p"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MetricTrainer", options);
            return;
        }

        Env env = new EnvBuilder(cmd)
                .build();
        Configurator c = env.getConfigurator();
        LocalSRMetric sr = c.get(LocalSRMetric.class,cmd.getOptionValue("m"), false);
        LocalPageDao lpd = c.get(LocalPageDao.class);

        String[] pa = cmd.getOptionValues('p');

        String phrase = "";
        for (String s : pa){
            phrase+=s+" ";
        }



        Language lang = Language.getByLangCode("simple");
        ExplanationFormatter expf = new ExplanationFormatter(lpd);



        SRResultList resultList = sr.mostSimilar(new LocalString(lang, phrase), 5);
        System.out.println("Most similar to "+phrase+":");
        for (int i=0; i<resultList.numDocs(); i++){
            System.out.println("#" + (i + 1));
            localPrintResult(resultList.get(i),lang,lpd, expf);
        }

    }
}
