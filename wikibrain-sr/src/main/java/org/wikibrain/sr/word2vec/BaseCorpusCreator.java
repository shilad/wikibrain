package org.wikibrain.sr.word2vec;

import gnu.trove.TCollections;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.io.FileUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public abstract class BaseCorpusCreator {
    private static final Logger LOG = Logger.getLogger(BaseCorpusCreator.class.getName());

    private final Language language;

    private final TLongIntMap wordLengths = new TLongIntHashMap();
    private final TLongIntMap wordCounts = TCollections.synchronizedMap(new TLongIntHashMap());

    private BufferedWriter words;
    private BufferedWriter textCorpus;
    private BufferedWriter hashCorpus;

    public BaseCorpusCreator(Language language) {
        this.language = language;
    }

    /**
     * @return A list of Strings in the corpus.
     * Each string should be at least sentence granularity.
     * They could be a higher level (paragraph, document).
     */
    public abstract Iterator<String> getCorpus() throws DaoException;

    public void write(File dir) throws IOException, DaoException {
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();
        words = WpIOUtils.openWriter(new File(dir, "words.txt"));
        textCorpus = WpIOUtils.openWriter(new File(dir, "textCorpus.txt"));
        hashCorpus = WpIOUtils.openWriter(new File(dir, "hashCorpus.txt"));
        ParallelForEach.iterate(getCorpus(), new Procedure<String>() {
            @Override
            public void call(String text) throws Exception {
                for (String sentence : getSentences(text)) {
                    processSentence(sentence, textCorpus, hashCorpus);
                }
            }
        });
        textCorpus.close();
        hashCorpus.close();
        words.close();

        LOG.info("writing " + wordCounts.size() + " counts...");
        BufferedWriter writer = WpIOUtils.openWriter(new File(dir, "counts.txt"));
        for (long hash : wordCounts.keys()) {
            writer.write(hash + " " + wordCounts.get(hash) + " " + wordLengths.get(hash) + "\n");
        }
        writer.close();
    }

    private void processSentence(String sentence, BufferedWriter textCorpus, BufferedWriter hashCorpus) throws IOException {
        TLongObjectHashMap<String> wordHashes = new TLongObjectHashMap<String>();
        StringBuilder wordLine = new StringBuilder();
        StringBuilder hashLine = new StringBuilder();
        TLongIntMap sentenceHashCounts = new TLongIntHashMap();

        for (String word : getWords(sentence)) {
            word = word.trim();
            if (word.length() == 0) {
                continue;
            }
            if (word.contains("\n")) {
                throw new IllegalStateException();
            }
            long h = Word2VecUtils.hashWord(word);
            wordHashes.put(h, word);
            if (hashLine.length() > 0) {
                hashLine.append(' ');
                wordLine.append(' ');
            }
            hashLine.append("" + h);
            wordLine.append(word);
            sentenceHashCounts.adjustOrPutValue(h, 1, 1);
        }
        hashLine.append('\n');
        wordLine.append('\n');
        synchronized (hashCorpus) {
            hashCorpus.write(hashLine.toString());
        }
        synchronized (textCorpus) {
            textCorpus.write(wordLine.toString());
        }
        synchronized (wordCounts) {
            for (long hash : sentenceHashCounts.keys()) {
                if (hash != 0) {
                    int c = sentenceHashCounts.get(hash);
                    if (!wordCounts.containsKey(hash)) {
                        String word = wordHashes.get(hash);
                        wordLengths.put(hash, word.length());
                        words.write("" + hash + " " + word + "\n");
                    }
                    wordCounts.adjustOrPutValue(hash, c, c);
                }
            }
        }
    }

    private List<String> getSentences(String text) {
        List<String> sentences = new ArrayList<String>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                sentences.add(text.substring(lastBoundary, boundary));
            }
            lastBoundary = boundary;
        }
        return sentences;
    }

    private List<String> getWords(String text) {
        List<String> sentences = new ArrayList<String>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getWordInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                String word = text.substring(lastBoundary, boundary);
                if (word.length() > 0 && Character.isLetterOrDigit(word.charAt(0))) {
                    sentences.add(word);
                }
            }
            lastBoundary = boundary;
        }
        return sentences;
    }
}
