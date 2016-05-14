package org.wikibrain.sr.wikify;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.phrases.LinkProbabilityDao;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Shilad Sen
 */
public class PlainTextCorpusCreator extends BaseCorpusCreator{
    private static final Logger LOG = LoggerFactory.getLogger(PlainTextCorpusCreator.class);

    private final File file;
    private int maxPages = Integer.MAX_VALUE;

    public PlainTextCorpusCreator(Language language, Wikifier wikifier, LocalPageDao lpd, LinkProbabilityDao probabilityDao, File inputFile) {
        super(language, lpd, wikifier, probabilityDao);
        this.file = inputFile;
        if (!file.isFile()) {
            throw new IllegalArgumentException("Plaintext corpus " + file + " does not exist");
        }
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    public Iterator<IdAndText> getCorpus() throws DaoException {
        try {
            return new ClosingLineIterator(
                    IOUtils.lineIterator(
                            WpIOUtils.openReader(file)));
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    public static class ClosingLineIterator implements Iterator<IdAndText> {
        private LineIterator iter;

        public ClosingLineIterator(LineIterator iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            LineIterator i = iter;
            if (i == null) {
                return false;
            } else if (i.hasNext()) {
                return true;
            } else {
                i.close();
                iter = null;
                return false;
            }
        }

        @Override
        public IdAndText next() {
            return new IdAndText(-1, iter.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("input")
                        .withDescription("input output file (existing data will be lost)")
                        .create("i"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("corpus output directory (existing data will be lost)")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("max-articles")
                        .withDescription("Maximum number of articles to process")
                        .create("x"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("WikiTextCorpusCreator", options);
            return;
        }
        Env env = new EnvBuilder(cmd).build();
        RawPageDao rpd = env.getConfigurator().get(RawPageDao.class);
        LocalPageDao lpd = env.getConfigurator().get(LocalPageDao.class);
        Language lang = env.getLanguages().getDefaultLanguage();
        Wikifier wikifier = env.getComponent(Wikifier.class, lang);
        LinkProbabilityDao linkProbabilityDao = env.getComponent(LinkProbabilityDao.class, lang);

        PlainTextCorpusCreator creator = new PlainTextCorpusCreator(
                lang, wikifier, lpd, linkProbabilityDao, new File(cmd.getOptionValue("i")));
        if (cmd.hasOption("x")) {
            creator.setMaxPages(Integer.valueOf(cmd.getOptionValue("x")));
        }
        File output = new File(cmd.getOptionValue("o"));
        creator.write(output);
    }
}
