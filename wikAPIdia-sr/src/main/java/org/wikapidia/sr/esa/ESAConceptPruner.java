package org.wikapidia.sr.esa;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Shilad Sen
 */
public class ESAConceptPruner {
    private static final int DEFAULT_MAX_CONCEPTS = 50000;

    private Language lang;
    private LocalLinkDao linkDao;
    private LocalPageDao pageDao;

    public ESAConceptPruner(Language lang, LocalLinkDao linkDao, LocalPageDao pageDao) {
        this.lang = lang;
        this.linkDao = linkDao;
        this.pageDao = pageDao;
    }

    public TIntSet prune(int maxConcepts) throws DaoException {
        DaoFilter filter = new DaoFilter()
                .setNameSpaces(NameSpace.ARTICLE)
                .setLanguages(lang)
                .setRedirect(false)
                .setDisambig(false);

        int numArticles = pageDao.getCount(filter);
        int numStopArticles =getNumStopConcepts(numArticles);
        Leaderboard mostLinked = new Leaderboard(maxConcepts + numStopArticles);

        for (LocalPage lp : (Iterable<LocalPage>)pageDao.get(filter)) {
            if (lp == null) {
                continue;
            }

            // the first few checks should be unnecessary, but let's be safe
            if ((lp.getNameSpace() != NameSpace.ARTICLE)
            ||  (lp.isDisambig())
            ||  (lp.isRedirect())
            ||  (isBlacklisted(lp))
            ||  (isList(lp))) {
                continue;
            }

            DaoFilter query = new DaoFilter().setLanguages(lang).setDestIds(lp.getLocalId());
            int n = linkDao.getCount(query);
            mostLinked.tallyScore(lp.getLocalId(), n);
        }

        SRResultList sorted = mostLinked.getTop();
        sorted.sortDescending();
        TIntSet result = new TIntHashSet();
        for (int i = 0; i < sorted.numDocs(); i++) {
            int id = sorted.getId(i);
            double nlinks = sorted.getScore(i);
            if (i < numStopArticles) {
                System.out.println("skipping highly linked 'stop' page " +
                        pageDao.getById(lang, id) +
                        " (" + nlinks + " links)");
            } else {
                result.add(sorted.getId(i));
            }
        }
        return result;
    }

    /**
     * TODO: make this multi-lingual.
     * @param lp
     * @return
     */
    private static final Pattern[] TITLE_BLACKLIST = new Pattern[] {
            // articles starting with a year
            Pattern.compile("^[0-9]{4}.*"),

            // articles starting with a month
            Pattern.compile("^(January|February|March|April|May|June|July|August|September|October|November|December)"),

            // articles that are just digits
            Pattern.compile("^[0-9]+$"),
    };

    private boolean isBlacklisted(LocalPage lp) {
        String title = lp.getTitle().getCanonicalTitle();
        for (Pattern p : TITLE_BLACKLIST) {
            if (p.matcher(title).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * TODO: make multi lingual
     * @param lp
     * @return
     */
    private boolean isList(LocalPage lp) {
        return lp.getTitle().getCanonicalTitle().toLowerCase().startsWith("list");
    }

    /**
     * Ridiculous heuristic: number of stop concepts = 2 * cubed-root-of(num-articles)
     * For simple english (175K articles), this is about 100
     * For english (4M articles), this is about 300
     * @param numArticles
     * @return
     */
    public int getNumStopConcepts(int numArticles) {
        return (int) (Math.pow(numArticles, 0.33333) * 2);
    }


    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Options options = new Options();

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output-dir")
                        .withDescription("directory to output concept mapping to")
                        .create("d"));

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("max-concepts")
                        .withDescription("maximum number of concepts")
                        .create("x"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ESAConceptPruner", options);
            return;
        }

        int maxConcepts = cmd.hasOption("x")
                ? Integer.valueOf(cmd.getOptionValue("x"))
                : DEFAULT_MAX_CONCEPTS;

        Env env = new EnvBuilder(cmd).build();
        Configurator c = env.getConfigurator();

        LocalLinkDao linkDao = c.get(LocalLinkDao.class);
        LocalPageDao pageDao = c.get(LocalPageDao.class);

        File parentDir = new File(cmd.getOptionValue("d"));
        if (!parentDir.isDirectory()) {
            FileUtils.deleteQuietly(parentDir);
            parentDir.mkdirs();
        }
        for (Language lang : env.getLanguages()) {
            ESAConceptPruner pruner = new ESAConceptPruner(lang, linkDao, pageDao);
            File path = new File(parentDir, lang.getLangCode() + ".txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            for (int wpId : pruner.prune(maxConcepts).toArray()) {
                writer.write(wpId + "\n");
            }
            writer.close();
        }
    }
}
