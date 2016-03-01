package org.wikibrain.sr.wikify;

import org.apache.commons.cli.*;
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class WikiTextCorpusCreator extends BaseCorpusCreator{
    private static final Logger LOG = LoggerFactory.getLogger(WikiTextCorpusCreator.class);

    private final Language language;
    private final RawPageDao dao;
    private int maxPages = Integer.MAX_VALUE;

    public WikiTextCorpusCreator(Language language, Wikifier wikifier, RawPageDao dao, LocalPageDao lpd, LinkProbabilityDao probabilityDao) {
        super(language, lpd, wikifier, probabilityDao);
        this.language = language;
        this.dao = dao;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    public Iterator<IdAndText> getCorpus() throws DaoException {
        DaoFilter filter = new DaoFilter()
                .setRedirect(false)
                .setDisambig(false)
                .setLanguages(language)
                .setLimit(maxPages);
        Iterator<RawPage> iter = dao.get(filter).iterator();
        return new RawPageTextIterator(iter);
    }

    public static class RawPageTextIterator implements Iterator<IdAndText> {
        private final Iterator<RawPage> iter;
        private static IdAndText buffer = null;

        public RawPageTextIterator(Iterator<RawPage> iter) {
            this.iter = iter;
            this.fillBuffer();
        }

        @Override
        public boolean hasNext() {
            return (buffer != null);
        }

        @Override
        public IdAndText next() {
            IdAndText result = buffer;
            if (buffer != null) {
                buffer = null;
                fillBuffer();
            }
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void fillBuffer() {
            while (buffer == null && iter.hasNext()) {
                RawPage rp = iter.next();
                if (rp != null) {
                    try {
                        String text = rp.getPlainText(false);
                        if (text != null && text.trim().length() > 0) {
                            buffer = new IdAndText(rp.getLocalId(), text.trim());
                        }
                    } catch (Exception e) {
                        LOG.warn("Error when extracting text from: " + rp.getTitle());
                    }
                }
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Options options = new Options();
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
        LinkProbabilityDao linkProbabilityDao =env.getComponent(LinkProbabilityDao.class, lang);
        Wikifier wikifier = env.getComponent(Wikifier.class, lang);

        WikiTextCorpusCreator creator = new WikiTextCorpusCreator(lang, wikifier, rpd, lpd, linkProbabilityDao);
        if (cmd.hasOption("x")) {
            creator.setMaxPages(Integer.valueOf(cmd.getOptionValue("x")));
        }
        File output = new File(cmd.getOptionValue("o"));
        creator.write(output);
    }
}
