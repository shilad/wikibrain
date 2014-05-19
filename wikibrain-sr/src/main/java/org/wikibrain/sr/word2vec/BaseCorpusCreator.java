package org.wikibrain.sr.word2vec;

import gnu.trove.TCollections;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.io.FileUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.sr.wikify.Wikifier;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Shilad Sen
 */
public abstract class BaseCorpusCreator {
    private static final Logger LOG = Logger.getLogger(BaseCorpusCreator.class.getName());

    private final Language language;
    private final StringTokenizer tokenizer = new StringTokenizer();

    private final TLongIntMap wordLengths = new TLongIntHashMap();
    private final TLongIntMap wordCounts = TCollections.synchronizedMap(new TLongIntHashMap());
    private final Wikifier wikifier;

    private BufferedWriter dictionary;
    private BufferedWriter corpus;

    public BaseCorpusCreator(Language language, Wikifier wikifier) {
        this.language = language;
        this.wikifier = wikifier;
    }

    /**
     * @return A list of Strings in the corpus.
     * Each string should be at least sentence granularity.
     * They could be a higher level (paragraph, document).
     */
    public abstract Iterator<IdAndText> getCorpus() throws DaoException;

    public void write(File dir) throws IOException, DaoException {
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();
        dictionary = WpIOUtils.openWriter(new File(dir, "dictionary.txt"));
        corpus = WpIOUtils.openWriter(new File(dir, "corpus.txt"));
        ParallelForEach.iterate(getCorpus(), new Procedure<IdAndText>() {
            @Override
            public void call(IdAndText text) throws Exception {
                processText(text);
            }
        });
        corpus.close();
        dictionary.close();

        LOG.info("writing " + wordCounts.size() + " counts...");
        BufferedReader dict = WpIOUtils.openBufferedReader(new File(dir, "dictionary.txt"));
        BufferedWriter writer = WpIOUtils.openWriter(new File(dir, "words.txt"));
        while (true) {
            String line = dict.readLine();
            if (line == null) {
                break;
            }
            String phrase = line.trim();
            long hash = Word2VecUtils.hashWord(phrase);
            writer.write("" + wordCounts.get(hash) + " " + phrase + "\n");
        }
        writer.close();
        dict.close();
    }

    private void processText(IdAndText text) throws IOException, DaoException {
        List<LocalLink> mentions;
        if (text.getId() >= 0) {
            mentions = wikifier.wikify(text.getId(), text.getText());
        } else {
            mentions = wikifier.wikify(text.getText());
        }
        StringBuilder document = new StringBuilder();
        for (Token sentence : tokenizer.getSentenceTokens(language, text.getText())) {
            String processSentence = processSentence(sentence, mentions);
            if (processSentence != null) {
                document.append(processSentence);
            }
        }

        synchronized (corpus) {
            corpus.write(document.toString() + "\n\n");
        }

        countTokens(document.toString());
    }

    Pattern PATTERN_ID = Pattern.compile("^(.*):ID(\\d+)$");

    private void countTokens(String document) throws IOException {
        String [] phrases = document.split(" +");
        synchronized (wordCounts) {
            for (String phrase : phrases) {
                Matcher m = PATTERN_ID.matcher(phrase);
                if (m.matches()) {
                    phrase = m.group(1);
                }
                long hash = Word2VecUtils.hashWord(phrase);
                if (hash != 0) {
                    if (!wordCounts.containsKey(hash)) {
                        wordLengths.put(hash, phrase.length());
                        this.dictionary.write("" + hash + " " + phrase + "\n");
                    }
                    wordCounts.adjustOrPutValue(hash, 1, 1);
                }
            }
        }
    }


    private String processSentence(Token sentence, List<LocalLink> mentions) throws IOException {
        List<Token> words = tokenizer.getWordTokens(language, sentence);
        if (words.isEmpty()) {
            return null;
        }

        // Accumulators
        TLongObjectHashMap<String> wordHashes = new TLongObjectHashMap<String>();
        StringBuilder wordLine = new StringBuilder();
        TLongIntMap sentenceHashCounts = new TLongIntHashMap();

        // Process each word token
        // Warning: If mentions do not align with sentence tokens, this will break...
        for (int m = 0, w = 0; w < words.size(); w++) {
            Token token = words.get(w);

            // Advance mention while it starts before the current token
            while (m < mentions.size() && mentions.get(m).getLocation() < token.getBegin()) {
                m++;
            }

            String phrase = token.getToken();

            // If start of mention occurs in token, advance tokens as necessary
            if (m < mentions.size() && mentions.get(m).getLocation() < token.getEnd()) {
                int end = mentions.get(m).getLocation() + mentions.get(m).getAnchorText().length();

                // While next word begins before mention ends, append next word
                while (w+1 < words.size() && words.get(w+1).getBegin() < end) {
                    if (phrase.length() > 0) {
                        phrase += "_";
                    }
                    w++;
                    phrase += words.get(w).getToken();
                }
                phrase += ":ID" + mentions.get(m).getDestId();
            }

            phrase = phrase.trim();
            if (phrase.length() == 0) {
                continue;
            }
            if (phrase.contains("\n")) {
                throw new IllegalStateException();
            }
            long h = Word2VecUtils.hashWord(phrase);
            wordHashes.put(h, phrase);
            if (wordLine.length() > 0) {
                wordLine.append(' ');
            }
            wordLine.append(phrase);
            sentenceHashCounts.adjustOrPutValue(h, 1, 1);
        }
        wordLine.append('\n');

        return wordLine.toString();
    }
}
