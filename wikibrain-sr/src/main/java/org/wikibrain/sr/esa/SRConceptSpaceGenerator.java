package org.wikibrain.sr.esa;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * @author Shilad Sen
 */
public class SRConceptSpaceGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SRConceptSpaceGenerator.class);

    private final Language lang;
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final int numArticles;
    private int maxConcepts = -1;

    public SRConceptSpaceGenerator(Language lang, LocalLinkDao linkDao, LocalPageDao pageDao) throws DaoException {
        this.lang = lang;
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.numArticles = pageDao.getCount(getFilter());
    }

    public DaoFilter getFilter() {
        return new DaoFilter()
                .setNameSpaces(NameSpace.ARTICLE)
                .setLanguages(lang)
                .setRedirect(false)
                .setDisambig(false);
    }

    public TIntSet getConcepts() throws DaoException {
        int numStopArticles =getNumStopConcepts();
        Leaderboard mostLinked = new Leaderboard(getMaxConcepts() + numStopArticles);

        for (LocalPage lp : (Iterable<LocalPage>)pageDao.get(getFilter())) {
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
        TIntSet result = new TIntHashSet();
        for (int i = 0; i < sorted.numDocs(); i++) {
            if (i < numStopArticles) {
//                int id = sorted.getId(i);
//                double nlinks = sorted.getScore(i);
//                System.out.println("skipping highly linked 'stop' page " +
//                        pageDao.getById(lang, id) +
//                        " (" + nlinks + " links)");
            } else {
                result.add(sorted.getId(i));
            }
        }
        return result;
    }

    public void writeConcepts(File path) throws DaoException, IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        for (int wpId : getConcepts().toArray()) {
            writer.write(wpId + "\n");
        }
        writer.close();
    }

    /**
     * TODO: make this multi-lingual.
     * @param lp
     * @return
     */
    private static final Pattern[] TITLE_BLACKLIST = new Pattern[] {
            // articles starting with a year
            Pattern.compile("^[0-9]{4} .*"),

            // articles starting with a month
            Pattern.compile("^(January|February|March|April|May|June|July|August|September|October|November|December).*"),

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
     * @return
     */
    public int getNumStopConcepts() {
        return (int) (Math.pow(numArticles, 0.33333) * 2);
    }

    /**
     * Simple heuristic for number of max concepts.
     * For simple english (175K articles) default is about 55K
     * For english (4M articles), default is about 158K
     * @return
     */
    public int getMaxConcepts() {
        if (maxConcepts < 0) {
            return (int) (Math.pow(numArticles, 0.33333) * 1000);
        } else {
            return maxConcepts;
        }
    }

    public void setMaxConcepts(int maxConcepts) {
        this.maxConcepts = maxConcepts;
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Options options = new Options();

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
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
            new HelpFormatter().printHelp("SRConceptSpaceGenerator", options);
            return;
        }


        Env env = new EnvBuilder(cmd).build();
        Configurator c = env.getConfigurator();

        LocalLinkDao linkDao = c.get(LocalLinkDao.class);
        LocalPageDao pageDao = c.get(LocalPageDao.class);


        File parentDir = new File(env.getConfiguration().get().getString("sr.concepts.path"));
        if (cmd.hasOption("d")) {
            parentDir = new File(cmd.getOptionValue("d"));
        }
        if (!parentDir.isDirectory()) {
            FileUtils.deleteQuietly(parentDir);
            parentDir.mkdirs();
        }
        for (Language lang : env.getLanguages()) {
            SRConceptSpaceGenerator pruner = new SRConceptSpaceGenerator(lang, linkDao, pageDao);
            if (cmd.hasOption("x")) {
                pruner.setMaxConcepts(Integer.valueOf(cmd.getOptionValue("x")));
            }
            File path = new File(parentDir, lang.getLangCode() + ".txt");
            pruner.writeConcepts(path);
        }
    }
}
