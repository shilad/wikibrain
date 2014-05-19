package org.wikibrain.sr.word2vec;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongIntProcedure;
import org.apache.commons.io.IOUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.nlp.NGramCreator;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/**
 * A class to remember counts for unigrams and (optionally) bigrams.
 *
 * Uses a hashing trick so that a word's counts can be kept in 12 bytes of memory.
 * All count* methods are threadsafe.
 *
 * All get* methods are threadsafe as long as they aren't
 * called at the same time as count* methods.
 *
 * Also remembers the number of mentions for each article.
 * A mention must be in the format "foo:ID3424" where foo is the phrase
 * mentioning the article.
 *
 * @author Shilad Sen
 */
public class Dictionary implements Closeable {


    public static enum WordStorage {
        ON_DISK,
        IN_MEMORY,
        NONE
    }

    private final Language language;
    private boolean containsMentions = true;
    private boolean countBigrams = true;
    private final WordStorage wordStorage;

    private AtomicLong totalWords = new AtomicLong();
    private final TLongIntMap unigramCounts = new TLongIntHashMap();
    private final TLongIntMap bigramCounts = new TLongIntHashMap();

    private StringTokenizer tokenizer = new StringTokenizer();
    private NGramCreator nGramCreator = new NGramCreator();

    private BufferedWriter wordWriter;
    private File wordFile;

    public Dictionary(Language language) {
        this(language, WordStorage.NONE);
    }

    public Dictionary(Language language, WordStorage wordMode) {
        this.language = language;
        this.wordStorage = wordMode;
        if (wordMode == WordStorage.ON_DISK) {
            try {
                wordFile = File.createTempFile("words", ".txt");
                wordFile.deleteOnExit();
                wordFile.delete();
                wordWriter = WpIOUtils.openWriter(wordFile);
            } catch (IOException e) {
                throw new RuntimeException(e);  // shouldn't happen for a temp file...
            }
        }
    }

    /**
     * Map of Wikipedia article id -> number of mentions in unigrams.
     * Only calculated if containsMentions is true.
     */
    private final TIntIntMap mentionCounts = new TIntIntHashMap();

    /**
     * Map from word hashes to actual words.
     * Only maintained if "rememberWords" is true.
     */
    private final TLongObjectMap<String> words = new TLongObjectHashMap<String>();

    /**
     *
     * Counts words in a file that have not been normalized.
     * I.e. the separators between words are those traditionally found in plain text.
     * Note that each line is presumed to be a sentence, so bigrams that span lines are not allowed.
     *
     * @param corpus
     * @throws IOException
     */
    public void countRawFile(File corpus) throws IOException {
        BufferedReader reader = WpIOUtils.openBufferedReader(corpus);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            countRawText(line);
        }
    }

    /**
     *
     * Counts words in a file that have not been normalized.
     * I.e. the separators between words are only spaces without punctuation.
     * Note that each line is presumed to be a sentence, so bigrams that span lines are not allowed.
     *
     * @param corpus
     * @throws IOException
     */
    public void countNormalizedFile(File corpus) throws IOException {
        BufferedReader reader = WpIOUtils.openBufferedReader(corpus);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            countRawText(line);
        }
    }

    /**
     * Counts words that have not been normalized. I.e. the separators between words
     * are those traditionally found in plain text.
     *
     * @param text
     */
    public void countRawText(String text) {
        countWords(tokenizer.getWords(language, text));
    }

    /**
     * Counts words that have not been normalized. I.e. words are separated ONLY by spaces.
     *
     * @param text
     */
    public void countNormalizedText(String text) {
        countWords(Arrays.asList(text.split(" +")));
    }

    private void countWords(List<String> tokens) {
        for (String word : tokens) {
            countUnigram(word);
        }
        if (countBigrams) {
            for (String bigram : nGramCreator.getNGrams(tokens, 2, 2)) {
                countBigram(bigram);
            }
        }
    }

    /**
     * Increments the count for a particular unigram.
     *
     * If this.countMentions is true, it scans for, counts,
     * and removes any article mentions "foo:ID342234" => "foo"
     *
     * If rememberWords is true, and this word hasn't been seen before,
     * it remembers the word.
     *
     * @param word
     */
    public void countUnigram(String word) {
        word = word.trim();
        if (word.isEmpty()) {
            return;
        }
        if (containsMentions) {
            Matcher m = Word2VecUtils.PATTERN_ID.matcher(word);
            if (m.matches()) {
                word = m.group(1);
                int wpId = Integer.valueOf(m.group(2));
                mentionCounts.adjustOrPutValue(wpId, 1, 1);
            }
        }
        long hash = getHash(word);
        if (wordStorage == WordStorage.IN_MEMORY) {
            synchronized (words) {
                if (!words.containsKey(hash)) {
                    words.put(hash, word);
                }
            }
        }
        synchronized (unigramCounts) {
            unigramCounts.adjustOrPutValue(hash, 1, 1);
        }
        totalWords.incrementAndGet();
    }

    /**
     * Counts a bigram.
     * @param word
     */
    public void countBigram(String word) {
        word = word.trim();
        if (word.isEmpty()) {
            return;
        }
        if (containsMentions) {
            Matcher m = Word2VecUtils.PATTERN_ID.matcher(word);
            if (m.matches()) {
                word = m.group(2);
            }
        }
        synchronized (bigramCounts) {
            bigramCounts.adjustOrPutValue(getHash(word), 1, 1);
        }
    }

    /**
     * Writes all unigrams and mentions in a dictionary to a file.
     * @param output
     * @throws IOException
     */
    public void write(File output) throws IOException {
        write(output, 1);
    }

    /**
     * Writes all unigrams with at least minCount frequency and all mentions to a file.
     * @param output
     * @param minCount
     * @throws IOException
     */
    public void write(File output, int minCount) throws IOException {
        if (wordStorage != WordStorage.ON_DISK) {
            // TODO: implementing IN_MEMORY would be trivial.
            throw new UnsupportedOperationException();
        }
        IOUtils.closeQuietly(this);

        BufferedReader reader = WpIOUtils.openBufferedReader(this.wordFile);
        BufferedWriter writer = WpIOUtils.openWriter(output);
        writer.write("t " + totalWords.get() + "_\n");
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String phrase = line.trim();
            long hash = getHash(phrase);
            int c = unigramCounts.get(hash);
            if (c < minCount) {
                continue;
            }
            writer.write("w " + c + " " + phrase + "\n");
        }
        for (int wpId : mentionCounts.keys()) {
            writer.write("m " + wpId + " " + mentionCounts.get(wpId) + "\n");
        }
        writer.close();
        reader.close();
    }

    /**
     * Reads an entire unigram dictionary back from a file.
     * @param file
     * @throws IOException
     */
    public void read(File file) throws IOException {
        read(file, Integer.MAX_VALUE, 1);
    }

    /**
     * Reads unigrams from a file.
     *
     * The top (by frequency) maxWords unigrams are retained.
     * @param file
     * @param maxWords
     * @param minCount
     * @throws IOException
     */
    public void read(File file, int maxWords, int minCount) throws IOException {
        if (wordStorage == WordStorage.ON_DISK) {
            throw new UnsupportedOperationException("Cannot read into dictionaries using disk storage");
        }

        // Pass 1: store all available counts to figure out cutoff for tracking words
        TIntList counts = new TIntArrayList();
        BufferedReader reader = WpIOUtils.openBufferedReader(file);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split(" ", 3);
            if (tokens[0].equals("w")) {
                int c = Integer.valueOf(tokens[1]);
                if (c >= minCount) {
                    counts.add(c);
                }
            }
        }
        reader.close();
        counts.sort();
        counts.reverse();

        /**
         * Figure out threshold for saving words
         */
        int threshold = 0;
        int numSavedAtThreshold = Integer.MAX_VALUE;
        if (counts.size() > maxWords) {
            threshold = counts.get(maxWords-1);
            for (int i = maxWords-1; i >= 0; i--) {
                if (counts.get(i) == threshold) {
                    numSavedAtThreshold++;
                } else {
                    break;
                }
            }
        }

        /**
         * Restore words
         */
        reader = WpIOUtils.openBufferedReader(file);
        totalWords.set(0);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split(" ", 3);
            if (tokens[0].equals("w")) {
                int c = Integer.valueOf(tokens[1]);
                if (c < threshold || c < minCount) {
                    continue;
                }
                if (c == threshold && numSavedAtThreshold == 0) {
                    continue;
                }
                if (c == threshold) {
                    numSavedAtThreshold--;
                }
                String phrase = tokens[2].trim();
                int count = Integer.valueOf(tokens[1]);
                long hash = getHash(phrase);
                unigramCounts.put(hash, count);
                if (wordStorage == WordStorage.IN_MEMORY) {
                    words.put(hash, phrase);
                }
            } else if (tokens[0].equals("m")) {
                mentionCounts.put(Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
            } else if (tokens[0].equals("t")) {
                totalWords.set(Integer.valueOf(tokens[1]));
            } else {
                throw new IOException("unexpected line: " + line);
            }
        }
        reader.close();
        counts.sort();
        counts.reverse();

    }

    public int getUnigramCount(String word) {
        return unigramCounts.get(getHash(word));
    }

    public int getBigramCount(String word) {
        return bigramCounts.get(getHash(word));
    }

    public int getMentionCount(int wpId) {
        return mentionCounts.get(wpId);
    }

    public final long getHash(String ngram) {
        return Word2VecUtils.hashWord(ngram);
    }

    public long getTotalCount() {
        return totalWords.get();
    }

    public void setContainsMentions(boolean containsMentions) {
        this.containsMentions = containsMentions;
    }

    public void setCountBigrams(boolean countBigrams) {
        this.countBigrams = countBigrams;
    }

    public void setTokenizer(StringTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public void setCreator(NGramCreator creator) {
        this.nGramCreator = creator;
    }

    public int getNumUnigrams() {
        return unigramCounts.size();
    }

    public int getNumBigrams() {
        return bigramCounts.size();
    }

    public int getNumMentionedArticles() {
        return mentionCounts.size();
    }

    public List<String> getTopUnigrams(int n) {
        if (wordStorage != WordStorage.IN_MEMORY) {
            throw new UnsupportedOperationException("WordStorage must be in memory to return strings");
        }
        int threshold = 0;
        if (n < unigramCounts.size()) {
            int counts[] = unigramCounts.values();
            Arrays.sort(counts);
            threshold = counts[counts.length - n];
        }

        final List<String> top = new ArrayList<String>();
        final int finalThreshold = threshold;
        unigramCounts.forEachEntry(new TLongIntProcedure() {
            @Override
            public boolean execute(long hash, int count) {
                if (count >= finalThreshold) {
                    top.add(words.get(hash));
                }
                return true;
            }
        });

        Collections.sort(top, new Comparator<String>() {
            @Override
            public int compare(String w1, String w2) {
                int r = getUnigramCount(w2) - getUnigramCount(w1);
                // order is deterministic for testing ease
                if (r == 0) {
                    r = w1.compareTo(w2);
                }
                return r;
            }
        });

        if (top.size() > n) {
            return top.subList(0, n);
        } else {
            return top;
        }
    }

    @Override
    public void close() throws IOException {
        if (this.wordWriter != null) {
            wordWriter.close();
        }
    }
}
