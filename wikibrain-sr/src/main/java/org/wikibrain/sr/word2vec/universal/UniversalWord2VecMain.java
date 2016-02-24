package org.wikibrain.sr.word2vec.universal;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.nlp.Dictionary;
import org.wikibrain.sr.SRBuilder;
import org.wikibrain.sr.wikify.Corpus;
import org.wikibrain.sr.wikify.WbCorpusLineReader;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Shilad Sen
 */
public class UniversalWord2VecMain {
    private static final int OPTIMAL_FILE_SIZE = 50 * 1024 * 1024;

    private final Language lang;
    private final Env env;
    private final TIntIntMap concepts;

    UniversalWord2VecMain(Env env, Language lang) throws ConfigurationException, DaoException {
        this.env = env;
        this.lang = lang;
        UniversalPageDao univDao = env.getConfigurator().get(UniversalPageDao.class);
        Map<Language, TIntIntMap> allConcepts = univDao.getAllLocalToUnivIdsMap(new LanguageSet(lang));
        this.concepts = allConcepts.containsKey(lang) ? allConcepts.get(lang) : new TIntIntHashMap();
    }

    void create(String path) throws ConfigurationException, DaoException, WikiBrainException, IOException {
        SRBuilder builder = new SRBuilder(env, "word2vec", lang);
        builder.setSkipBuiltMetrics(true);
        builder.setCreateFakeGoldStandard(true);
        builder.build();

        FileUtils.forceDelete(path);
        FileUtils.forceMkdir(new File(path));
        Corpus c = env.getConfigurator().get(Corpus.class, "wikified", "language", lang.getLangCode());
        if (c == null) throw new IllegalStateException("Couldn't find wikified corpus for language " + lang);
        if (!c.exists()) {
            c.create();
        }


        RotatingWriter writer = new RotatingWriter(
                path + "/corpus." + lang.getLangCode() + ".",
                ".txt",
                OPTIMAL_FILE_SIZE);
        WbCorpusLineReader cr = new WbCorpusLineReader(c.getCorpusFile());
        for (WbCorpusLineReader.Line line : cr) {
            List<String> words = new ArrayList<String>();
            for (String word : line.getLine().split(" +")) {
                int mentionStart = word.indexOf(":/w/");
                if (mentionStart >= 0) {
                    Matcher m = Dictionary.PATTERN_MENTION.matcher(word.substring(mentionStart));
                    if (m.matches()) {
                        List<String> parts = new ArrayList<String>();
                        parts.add(makeWordToken(word.substring(0, mentionStart)));
                        int wpId2 = Integer.valueOf(m.group(3));
                        if (wpId2 >= 0) {
                            parts.add(word.substring(mentionStart + 1));
                            if (concepts.containsKey(wpId2)) {
                                parts.add("/c/" + concepts.get(wpId2));
                            }
                            Collections.shuffle(parts);
                        }
                        words.addAll(parts);
                    } else {
                        words.add(makeWordToken(word));
                    }
                } else {
                    words.add(makeWordToken(word));
                }
            }
            writer.write(StringUtils.join(words, " ") + "\n");
        }
        writer.close();
    }

    private String makeWordToken(String word) {
        return lang.getLangCode() + ":" + word;
    }

    static class RotatingWriter implements Closeable {
        private final String prefix;
        private final String suffix;
        private final int maxBytes;
        private int fileNum = 0;
        private int numBytes = 0;
        private BufferedWriter writer = null;

        RotatingWriter(String prefix, String suffix, int maxBytes) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.maxBytes = maxBytes;
        }

        void write(String text) throws IOException {
            possiblyRotateWriter();
            numBytes += text.getBytes("UTF-8").length;
            writer.write(text);
        }

        private void possiblyRotateWriter() throws IOException {
            if (writer == null || numBytes >= maxBytes) {
                if (writer != null) {
                    close();
                    fileNum++;
                    numBytes = 0;
                }
                writer = WpIOUtils.openWriter(String.format("%s%05d%s", prefix, fileNum, suffix));
            }
        }

        @Override
        public void close() throws IOException {
            if (writer != null) {
                IOUtils.closeQuietly(writer);
                writer = null;
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("corpus output directory (existing data will be lost)")
                        .create("o"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("UniversalWord2VecMain", options);
            return;
        }
        Env env = new EnvBuilder(cmd).build();
        for (Language l : env.getLanguages()) {
            UniversalWord2VecMain creator = new UniversalWord2VecMain(env, l);
            creator.create(cmd.getOptionValue("o") + "/" + l.getLangCode());
        }
    }
}
