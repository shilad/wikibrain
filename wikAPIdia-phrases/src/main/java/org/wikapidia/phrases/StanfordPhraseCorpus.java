package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.apache.commons.lang3.math.Fraction;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.dao.PhraseAnalyzerDao;
import org.wikapidia.utils.WpIOUtils;


import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Loads phrase to page files from Indexes files from
 * http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/
 * into a PhraseAnalyzer
 *
 * These files capture anchor text associated with web pages that link to Wikipedia.
 * Note that the pages with anchor text are not (usually) Wikipedia pages themselves.
 */
public class StanfordPhraseCorpus extends SimplePhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(StanfordPhraseCorpus.class.getName());
    private static final LanguageInfo EN = LanguageInfo.getByLangCode("simple");

    private final File path;

    public StanfordPhraseCorpus(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao, File path) {
        super(phraseDao, pageDao);
        this.path = path;
    }

    /**
     * Loads a single Stanford phrase file into the database.
     * This can safely be called for multiple files if it is chunked.
     * @throws IOException
     */
    @Override
    public void loadCorpus(PhraseAnalyzerDao dao) throws IOException {
        BufferedReader reader = WpIOUtils.openReader(path);
        long numLines = 0;
        long numLinesRetained = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (++numLines % 100000 == 0) {
                double p = 100.0 * numLinesRetained / numLines;
                LOG.info("processing line: " + numLines +
                        ", retained " + numLinesRetained +
                        "(" + new DecimalFormat("#.#").format(p) + "%)");
            }
            try {
                Entry e = new Entry(line);
                LocalPage lp = pageDao.getByTitle(
                        EN.getLanguage(),
                        new Title(e.article, EN),
                        NameSpace.ARTICLE);
                if (lp != null) {
                    dao.add(EN.getLanguage(), lp.getLocalId(), e.text, e.getNumEnglishLinks());
                    numLinesRetained++;
                }
            } catch (Exception e) {
                LOG.log(Level.FINEST, "Error parsing line " + line + ":", e);
            }
        }
    }


    /**
     * A single  entry corresponding to a line from a
     * dictionary.bz2 at http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/.
     *
     * Major components of an entry are:
     * - textual phrase
     * - concept (a wikipedia article)
     * - A variety of flags
     */
    private static final Pattern MATCH_ENTRY = Pattern.compile("([^\t]*)\t([0-9.e-]+) ([^ ]*)(| (.*))$");

    @Override
    protected Iterable<SimplePhraseAnalyzer.Entry> getCorpus() throws IOException, DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    class Entry {
        String text;
        float fraction;
        String article;
        String flags[];

        Entry(String line) {
            Matcher m = MATCH_ENTRY.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException("invalid concepts entry: '" + line + "'");
            }
            this.text = m.group(1);
            this.fraction = Float.valueOf(m.group(2));
            this.article = m.group(3);
            this.flags = m.group(4).trim().split(" ");
        }

        int getNumEnglishLinks() {
            for (String flag : flags) {
                if (flag.startsWith("W:")) {
                    return Fraction.getFraction(flag.substring(2)).getNumerator();
                }
            }
            return 0;
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseCorpus> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PhraseCorpus.class;
        }

        @Override
        public String getPath() {
            return "phrases.corpus";
        }

        @Override
        public PhraseCorpus get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("stanford")) {
                return null;
            }
            LocalPageDao dao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            File path = new File(config.getString("path"));
            return new StanfordPhraseCorpus(dao, path);
        }
    }
}
