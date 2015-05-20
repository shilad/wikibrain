package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.Fraction;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads phrase to page files from Indexes files from
 * http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/
 * into a PhraseAnalyzer
 *
 * These files capture anchor phrase associated with web pages that link to Wikipedia.
 * Note that the pages with anchor phrase are not (usually) Wikipedia pages themselves.
 */
public class StanfordPhraseAnalyzer extends BasePhraseAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(StanfordPhraseAnalyzer.class);
    private static final Language LANG_EN = Language.getByLangCode("en");
    private static final Language LANG_SIMPLE = Language.getByLangCode("simple");

    private final File path;
    private LanguageSet languages;

    public StanfordPhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao, PrunedCounts.Pruner<String> phrasePruner, PrunedCounts.Pruner<Integer> pagePruner, File path) {
        super(phraseDao, pageDao, phrasePruner, pagePruner);
        this.path = path;
    }

    /**
     * Loads a single Stanford phrase file into the database.
     * This can safely be called for multiple files if it is chunked.
     * @throws IOException
     */
    @Override
    protected Iterable<BasePhraseAnalyzer.Entry> getCorpus(LanguageSet langs) throws IOException, DaoException {
        for (Language l : langs) {
            if (l != LANG_EN && l != LANG_SIMPLE) {
                LOG.warn("Stanford only supports English and Simple English (not " + l + ")");
            }
        }
        this.languages = langs;
        return new Iterable<Entry>() {
            @Override
            public Iterator<Entry> iterator() {
                try {
                    return new Iter();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected class Iter implements Iterator<BasePhraseAnalyzer.Entry> {
        BufferedReader reader;
        List<Entry> buffer = new ArrayList<Entry>();
        boolean eof = false;

        public Iter() throws IOException {
            reader = WpIOUtils.openBufferedReader(path);
        }

        @Override
        public boolean hasNext() {
            fillBuffer();
            return !buffer.isEmpty();
        }

        @Override
        public BasePhraseAnalyzer.Entry next() {
            fillBuffer();
            if (buffer.isEmpty()) {
                return null;
            } else {
                return buffer.remove(0);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void fillBuffer() {
            if (!buffer.isEmpty() || eof) {
                return;
            }
            while (!eof && buffer.isEmpty()) {
                try {
                    parseNextLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    LOG.debug("Error parsing line:", e);
                }
            }
        }

        private void parseNextLine() throws IOException {
            if (!buffer.isEmpty()) throw new IllegalStateException();
            String line = reader.readLine();
            if (line == null) {
                IOUtils.closeQuietly(reader);
                eof = true;
                return;
            }
            Record r = new Record(line);
            for (Language l : Arrays.asList(LANG_EN, LANG_SIMPLE)) {
                if (languages.containsLanguage(l)) {
                    buffer.add(
                            new BasePhraseAnalyzer.Entry(
                                    l, r.article, r.phrase, r.getNumEnglishLinks()));
                }
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

    class Record {
        String phrase;
        float fraction;
        String article;
        String flags[];

        Record(String line) {
            Matcher m = MATCH_ENTRY.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException("invalid concepts entry: '" + line + "'");
            }
            this.phrase = m.group(1);
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

    public static void downloadDictionaryIfNecessary(Configuration conf) throws IOException, InterruptedException {
        String path = conf.get().getString("phrases.analyzer.stanford.path");
        String url = conf.get().getString("phrases.analyzer.stanford.url");
        File file = new File(path);
        File completed = new File(path + ".completed");

        if (!completed.isFile()) {
            LOG.info("downloading stanford dictionary...");
            FileDownloader downloader = new FileDownloader();
            downloader.download(new URL(url), file);
            FileUtils.touch(completed);
        }
    }

    public static class Provider extends org.wikibrain.conf.Provider<PhraseAnalyzer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PhraseAnalyzer.class;
        }

        @Override
        public String getPath() {
            return "phrases.analyzer";
        }

        @Override
        public PhraseAnalyzer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("stanford")) {
                return null;
            }
            PhraseAnalyzerDao paDao = getConfigurator().construct(
                    PhraseAnalyzerDao.class, name, config.getConfig("dao"),
                    new HashMap<String, String>());
            LocalPageDao lpDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            File path = new File(config.getString("path"));
            PrunedCounts.Pruner<String> phrasePruner = getConfigurator().construct(
                    PrunedCounts.Pruner.class, null, config.getConfig("phrasePruner"), null);
            PrunedCounts.Pruner<Integer> pagePruner = getConfigurator().construct(
                    PrunedCounts.Pruner.class, null, config.getConfig("pagePruner"), null);
            return new StanfordPhraseAnalyzer(paDao, lpDao, phrasePruner, pagePruner, path);
        }
    }
}
