package org.wikibrain.core.nlp;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongIntProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to remember counts for unigrams and (optionally) bigrams.
 *
 * This class uses a hashing trick so that a word's counts can be kept in 12
 * bytes of memory.
 *
 *
 * All methods that count words are mutually threadsafe.
 * All methods that return counts are mutually threadsafe.
 * The two types of methods cannot be mixed with thread safety, though.
 *
 * This class also remembers the number of mentions for each article.
 * A mention must be in the format "foo:/w/en/1000" or "foo:/w/en/1000/Hercule_Poirot"
 * where foo is the phrase mentioning the article and 1000 is the Wikipedia article id
 * of the article with title "Hercule_Poirot."
 *
 * @author Shilad Sen
 */
public class Dictionary implements Closeable {
    public static final int MAX_DICTIONARY_SIZE = 20000000;   // 20M unigrams + bigrams by default.
    public static int PRUNE_INTERVAL = 10000;   // Consider pruning every PRUNE_INTERVAL increments

    public static Logger LOG = LoggerFactory.getLogger(Dictionary.class);

    /**
     * Matches mentions like:
     * foo:/w/en/1000
     * foo:/w/en/1000/Hercule_Poirot
     */
    public static final Pattern PATTERN_MENTION = Pattern.compile("(.*?):/w/([^/]+)/(-?\\d+)(/[^ ]*($| ))?");

    /**
     * How should words be stored? IN_MEMORY requires much more memory.
     */
    public static enum WordStorage {
        ON_DISK,
        IN_MEMORY,
        NONE
    }

    private final Language language;

    /**
     * Whether the text to be tokenized contains mentions of articles.
     */
    private boolean containsMentions = true;

    /**
     * Whether bigrams should be counted.
     */
    private boolean countBigrams = false;


    private final WordStorage wordStorage;

    private AtomicLong totalWords = new AtomicLong();
    private AtomicLong totalBigrams = new AtomicLong();
    private AtomicLong totalNgrams = new AtomicLong();

    private final TLongIntMap unigramCounts = new TLongIntHashMap();
    private final TLongIntMap bigramCounts = new TLongIntHashMap();
    private final TLongIntMap ngramCounts = new TLongIntHashMap();

    private StringTokenizer tokenizer = new StringTokenizer();
    private NGramCreator nGramCreator = new NGramCreator();

    private BufferedWriter wordWriter;
    private File wordFile;

    /**
     * The maximum number of unigrams + bigrams (not including mentions)
     */
    private int maxDictionarySize = MAX_DICTIONARY_SIZE;

    /**
     * Things with less than this number of occurrences will be pruned if
     * the dictionary size exceeds maxDictionarySize.
     *
     * This is incremented BEFORE every pruning (e.g. the first pruning will have
     * minPruneCount = 2).
     */
    private int minPruneCount = 1;

    /**
     * Map of Wikipedia article id -> number of mentions in unigrams.
     * Only calculated if containsMentions is true.
     */
    private final TIntIntMap mentionCounts = new TIntIntHashMap();


    /**
     * Hashes of interesting ngrams.
     */
    private TLongSet interestingNGrams = null;
    private TLongSet interestingSubGrams = null;

    /**
     * Map from word hashes to actual words.
     * Only maintained if "rememberWords" is true.
     */
    private final TLongObjectMap<String> words = new TLongObjectHashMap<String>();

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

    public void setInterestingNgrams(Iterator<String> ngrams) {
        interestingSubGrams = new TLongHashSet();
        interestingNGrams = new TLongHashSet();
        while (ngrams.hasNext()) {
            List<String> words = tokenizer.getWords(language, ngrams.next());
            if (words.isEmpty()) {
                continue;
            }
            StringBuilder b = new StringBuilder();
            long hash = -1;
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append(words.get(i));
                hash = hashWord(b.toString());
                interestingSubGrams.add(hash);
            }
            interestingNGrams.add(hash);
        }
    }


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
        LineIterator lineIterator = FileUtils.lineIterator(corpus, "UTF-8");
        ParallelForEach.iterate(
                lineIterator,
                Math.min(3, WpThreadUtils.getMaxThreads()), // 3 seems the optimal number of threads here...
                1000,
                new Procedure<String>() {
                    @Override
                    public void call(String line) throws Exception {
                        countRawText(line);
                    }
                },
                Integer.MAX_VALUE);
        lineIterator.close();
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
        LineIterator lineIterator = FileUtils.lineIterator(corpus, "UTF-8");
        ParallelForEach.iterate(
                lineIterator,
                Math.min(3, WpThreadUtils.getMaxThreads()), // 3 seems the optimal number of threads here...
                1000,
                new Procedure<String>() {
                    @Override
                    public void call(String line) throws Exception {
                        countNormalizedText(line);
                    }
                },
                Integer.MAX_VALUE);
        lineIterator.close();
    }

    /**
     * Counts words that have not been normalized. I.e. the separators between words
     * are those traditionally found in plain text.
     *
     * @param text
     */
    public void countRawText(String text) {
        // Count and extract mentions if necessary.
        if (containsMentions) {
            Matcher m = PATTERN_MENTION.matcher(text);
            while (m.find()) {
                int wpId = Integer.valueOf(m.group(3));
                synchronized (mentionCounts) {
                    mentionCounts.adjustOrPutValue(wpId, 1, 1);
                }
            }
            text = PATTERN_MENTION.matcher(text).replaceAll("$1 ");
        }
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
        if (interestingNGrams != null) {
            countNgrams(tokens);
        }
    }

    /**
     * Increments the count for a particular unigram.
     *
     * If this.countMentions is true, it scans for, counts,
     * and removes any article mentions "foo:ID342234" -&gt; "foo"
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
            Matcher m = PATTERN_MENTION.matcher(word);
            if (m.matches()) {
                word = m.group(1);
                int wpId = Integer.valueOf(m.group(3));
                synchronized (mentionCounts) {
                    mentionCounts.adjustOrPutValue(wpId, 1, 1);
                }
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
        int n;
        synchronized (unigramCounts) {
            n = unigramCounts.adjustOrPutValue(hash, 1, 1);
        }
        if (n == 1 && wordStorage == WordStorage.ON_DISK) {
            try {
                wordWriter.write(word + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);  // shouldn't really happen
            }
        }
        if (totalWords.incrementAndGet() % PRUNE_INTERVAL == 0) {
            pruneIfNecessary();
        }
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
            Matcher m = PATTERN_MENTION.matcher(word);
            if (m.matches()) {
                word = m.group(1);
            }
        }
        long h = getHash(word);
        synchronized (bigramCounts) {
            bigramCounts.adjustOrPutValue(h, 1, 1);
        }
        if (totalBigrams.incrementAndGet() % PRUNE_INTERVAL == 0) {
            pruneIfNecessary();
        }
    }

    public void countNgrams(List<String> words) {
        for (int i = 0; i < words.size(); i++) {
            StringBuilder buffer = new StringBuilder();
            for (int j = i; j < words.size(); j++) {
                if (j > i) {
                    buffer.append(' ');
                }
                buffer.append(words.get(i));
                long hash = hashWord(buffer.toString());
                if (!interestingNGrams.contains(hash) && !interestingSubGrams.contains(hash)) {
                    break;
                }
            }

        }
    }

    public synchronized void pruneIfNecessary() {
        while (true) {
            int n1, n2;
            synchronized (unigramCounts) {
                n1 = unigramCounts.size();
            }
            synchronized (bigramCounts) {
                n2 = bigramCounts.size();
            }
            if (n1 + n2 <= maxDictionarySize) {
                return;
            }
            minPruneCount++;
            LOG.info("pruning dictionary entries with frequency less than " + minPruneCount);
            synchronized (unigramCounts) {
                unigramCounts.retainEntries(new TLongIntProcedure() {
                    @Override
                    public boolean execute(long hash, int count) {
                        return (count >= minPruneCount);
                    }
                });
                n1 = unigramCounts.size();
            }
            synchronized (bigramCounts) {
                bigramCounts.retainEntries(new TLongIntProcedure() {
                    @Override
                    public boolean execute(long hash, int count) {
                        return (count >= minPruneCount);
                    }
                });
                n2 = bigramCounts.size();
            }
            LOG.info("after pruning dictionary size is " + (n1 + n2));
            // TODO: clear out words, but we need a triple lock... ugh.
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
        if (wordStorage == WordStorage.NONE) {
            throw new UnsupportedOperationException();
        }

        IOUtils.closeQuietly(this);
        BufferedWriter writer = WpIOUtils.openWriter(output);
        writer.write("t " + totalWords.get() + " _\n");

        if (wordStorage == WordStorage.ON_DISK) {
            BufferedReader reader = WpIOUtils.openBufferedReader(this.wordFile);
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
            reader.close();
        } else if (wordStorage == WordStorage.IN_MEMORY) {
            for (String phrase : words.valueCollection()) {
                long hash = getHash(phrase);
                int c = unigramCounts.get(hash);
                if (c < minCount) {
                    continue;
                }
                writer.write("w " + c + " " + phrase + "\n");
            }
        } else {
            throw new IllegalStateException();
        }
        for (int wpId : mentionCounts.keys()) {
            writer.write("m " + wpId + " " + mentionCounts.get(wpId) + "\n");
        }
        writer.close();
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
                totalWords.set(Long.valueOf(tokens[1]));
            } else {
                throw new IOException("unexpected line: " + line);
            }
        }
        reader.close();
        counts.sort();
        counts.reverse();

    }

    public int getUnigramCount(String bigram) {
        return unigramCounts.get(getHash(bigram));
    }

    public int getBigramCount(String word1, String word2) {
        return bigramCounts.get(getHash(word1 + " " + word2));
    }

    public int getBigramCount(String word) {
        return bigramCounts.get(getHash(word));
    }

    public int getMentionCount(int wpId) {
        return mentionCounts.get(wpId);
    }

    /**
     * Returns the number of times the article corresponding to the url was mentioned.
     * @param mentionUrl mention in the format /w/langCode/articleId/ArticleTitle
     * @return
     */
    public int getMentionCount(String mentionUrl) {
        if (!mentionUrl.startsWith("/w/")) {
            throw new IllegalArgumentException("format for mentionUrl must be /w/langCode/articleId/ArticleTitle");
        }
        String tokens[] = mentionUrl.split("/", 5);
        if (tokens.length != 5) {
            throw new IllegalArgumentException("format for mentionUrl must be /w/langCode/articleId/ArticleTitle");
        }
        return mentionCounts.get(Integer.valueOf(tokens[3]));
    }

    public final long getHash(String ngram) {
        return hashWord(ngram);
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

    /**
     * Return the n most frequently used unigrams, in decreasing order.
     * @param n
     * @return
     */
    public List<String> getFrequentUnigrams(int n) {
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

    /**
     * Return the n most frequently used unigrams and mentions, in decreasing order of frequency.
     *
     * Mentions are encoded as words with the format "/w/WikipediaId/ArticleTitle"
     *
     * @param lpd
     * @param maxWords
     * @param minWordFreq
     * @param minMentionFreq
     * @return
     */
    public List<String> getFrequentUnigramsAndMentions(LocalPageDao lpd, int maxWords, int minWordFreq, int minMentionFreq) throws DaoException {
        if (wordStorage != WordStorage.IN_MEMORY) {
            throw new UnsupportedOperationException("WordStorage must be in memory to return strings");
        }
        final int threshold;
        if (maxWords < unigramCounts.size()) {
            int counts[] = unigramCounts.values();
            Arrays.sort(counts);
            threshold = Math.max(minWordFreq, counts[counts.length - maxWords]);
        } else {
            threshold = 0;
        }

        final List<String> topWords = new ArrayList<String>();
        unigramCounts.forEachEntry(new TLongIntProcedure() {
            @Override
            public boolean execute(long hash, int count) {
                if (count >= threshold) {
                    topWords.add(words.get(hash));
                }
                return true;
            }
        });

        Collections.sort(topWords, new Comparator<String>() {
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

        List<String> result = topWords;
        if (result.size() > maxWords) result = result.subList(0, maxWords);
        for (int wpId : mentionCounts.keys()) {
            if (mentionCounts.get(wpId) >= minMentionFreq) {
                LocalPage lp = lpd.getById(language, wpId);
                if (lp != null) {
                    result.add(makeMentionUrl(lp));
                }
            }
        }

        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String w1, String w2) {
                int n1 = (w1.startsWith("/w/")) ? getMentionCount(w1) : getUnigramCount(w1);
                int n2 = (w2.startsWith("/w/")) ? getMentionCount(w2) : getUnigramCount(w2);
                int r = n2 - n1;
                // order is deterministic for testing ease
                if (r == 0) {
                    r = w1.compareTo(w2);
                }
                return r;
            }
        });

        return result;
    }

    private String makeMentionUrl(LocalPage page) {
        return "/w/" + language.getLangCode() + "/" + page.getLocalId() + "/" + page.getTitle().getCanonicalTitle().replaceAll(" ", "_");
    }


    @Override
    public void close() throws IOException {
        if (this.wordWriter != null) {
            wordWriter.close();
        }
    }

    /**
     * Returns a hashcode for a particular word.
     * The hashCode 0 will NEVER be returned.
     * @param w
     * @return
     */
    public static long hashWord(String w) {
        long h = MurmurHash.hash64(w);
        if (h == 0) h = 1;  // hack: h == 0 is reserved.
        return h;
    }

    public void setMaxDictionarySize(int maxDictionarySize) {
        this.maxDictionarySize = maxDictionarySize;
    }
}
