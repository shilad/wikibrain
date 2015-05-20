package org.wikibrain.phrases;

import com.google.code.externalsorting.ExternalSort;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of a phrase analyzer.
 * Concrete implementations extending this class need only implement a getCorpus() method.
 */
public abstract class BasePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(PhraseAnalyzer.class);

    /**
     * An entry in the phrase corpus.
     * Some implementations may have a local id.
     * Others will only have a title.
     */
    public static class Entry {
        Language language;
        int localId = -1;
        String title = null;
        String phrase;
        int count;

        public Entry(Language language, int localId, String phrase, int count) {
            this.language = language;
            this.localId = localId;
            this.phrase = phrase;
            this.count = count;
        }

        public Entry(Language language, String title, String phrase, int count) {
            this.language = language;
            this.title = title;
            this.phrase = phrase;
            this.count = count;
        }
    }

    private final PrunedCounts.Pruner<String> phrasePruner;
    private final PrunedCounts.Pruner<Integer> pagePruner;
    private final StringNormalizer normalizer;
    protected final PhraseAnalyzerDao phraseDao;
    protected final LocalPageDao pageDao;

    public BasePhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao, PrunedCounts.Pruner<String> phrasePruner, PrunedCounts.Pruner<Integer> pagePruner) {
        this.phrasePruner = phrasePruner;
        this.pagePruner = pagePruner;
        this.phraseDao = phraseDao;
        this.pageDao = pageDao;
        this.normalizer = phraseDao.getStringNormalizer();
    }

    /**
     * Concrete implementations must override this method to determine what phrases
     * are stored.
     *
     * @return
     * @throws IOException
     * @throws DaoException
     */
    protected abstract Iterable<Entry> getCorpus(LanguageSet langs) throws IOException, DaoException;

    /**
     * Loads a specific corpus into the dao.
     *
     * @throws DaoException
     * @throws IOException
     */
    @Override
    public int loadCorpus(LanguageSet langs) throws DaoException, IOException {
        // create temp files for storing corpus entries by phrase and local id.
        // these will ultimately be sorted to group together records with the same phrase / id.
        File byWpIdFile = File.createTempFile("wp_phrases_by_id", "txt");
        byWpIdFile.deleteOnExit();
        BufferedWriter byWpId = WpIOUtils.openWriter(byWpIdFile);
        File byPhraseFile = File.createTempFile("wp_phrases_by_phrase", "txt");
        byPhraseFile.deleteOnExit();
        BufferedWriter byPhrase = WpIOUtils.openWriter(byPhraseFile);

        // Iterate over each entry in the corpus.
        // Throws away entries in languages we don't care about.
        // Resolve titles to ids if necessary.
        // Write entries to the by phrase / id files.
        long numEntries = 0;
        long numEntriesRetained = 0;
        for (Entry e : getCorpus(langs)) {
            if (++numEntries % 1000000 == 0) {
                double p = 100.0 * numEntriesRetained / numEntries;
                LOG.info("processing entry: " + numEntries +
                        ", retained " + numEntriesRetained +
                        "(" + new DecimalFormat("#.#").format(p) + "%)");
            }
            if (!langs.containsLanguage(e.language)) {
                continue;
            }
            if (e.phrase == null || e.phrase.trim().isEmpty()) {
                continue;
            }
            if (e.title != null && e.localId < 0) {
                int localId = pageDao.getIdByTitle(new Title(e.title, e.language));
                e.localId = (localId <= 0) ? -1 : localId;
            }
            if (e.localId < 0) {
                continue;
            }
            numEntriesRetained++;
            e.phrase = e.phrase.replace("\n", " ").replace("\t", " ");
            // phrase is last because it may contain tabs.
            String line = e.language.getLangCode() + "\t" + e.localId + "\t" + e.count + "\t" + e.phrase + "\n";
            byPhrase.write(e.language.getLangCode() + ":" + normalize(e.language, e.phrase) + "\t" + line);
            byWpId.write(e.language.getLangCode() + ":" + e.localId + "\t" + line);
        }
        byWpId.close();
        byPhrase.close();

        // sort phrases by phrase / id and load them
        sortInPlace(byWpIdFile);
        loadFromFile(RecordType.PAGES, byWpIdFile, phrasePruner);
        sortInPlace(byPhraseFile);
        loadFromFile(RecordType.PHRASES, byPhraseFile, pagePruner);

        phraseDao.close();

        return (int) Math.min(Integer.MAX_VALUE, numEntriesRetained);
    }

    /**
     * Uses the string's normalizer, but replaces adjacent whitespace white a single space
     * @param lang
     * @param text
     * @return
     */
    private String normalize(Language lang, String text) {
        return normalizer.normalize(lang, text).replaceAll("\\s+", " ");
    }

    private static enum RecordType {
        PAGES, PHRASES
    }

    protected void loadFromFile(RecordType ltype, File input, PrunedCounts.Pruner pruner) throws IOException, DaoException {
        BufferedReader reader = WpIOUtils.openBufferedReader(input);
        String lastKey = null;

        int maxBufferSize = 1000;
        List<Entry> buffer = new ArrayList<Entry>();

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.split("\t", 5);
            if (tokens.length != 5) {
                LOG.warn("invalid line in file " + input + ": " + line);
                continue;
            }

            // if new id, write out buffer and clear it
            if (lastKey != null && !tokens[0].equals(lastKey)) {
                if (ltype == RecordType.PAGES) {
                    writePage(buffer, pruner);
                } else {
                    writePhrase(buffer, pruner);
                }
                buffer.clear();
            }
            Entry e = new Entry(
                    Language.getByLangCode(tokens[1]),
                    new Integer(tokens[2]),
                    tokens[4],
                    new Integer(tokens[3])
            );
            buffer.add(e);
            if (buffer.size() > maxBufferSize * 3 / 2) {
                LOG.warn("large buffer observed: " + buffer.size() + " for string " + lastKey);
                maxBufferSize = buffer.size();
            }
            lastKey = tokens[0];
        }
        if (ltype == RecordType.PAGES) {
            writePage(buffer, pruner);
        } else {
            writePhrase(buffer, pruner);
        }
    }

    protected void writePage(List<Entry> pageCounts, PrunedCounts.Pruner pruner) throws DaoException {
        if (pageCounts.isEmpty()) {
            return;
        }
        Language lang = pageCounts.get(0).language;
        int wpId = pageCounts.get(0).localId;
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (Entry e : pageCounts) {
            if (e.localId != wpId) throw new IllegalStateException();
            if (e.language != lang) throw new IllegalStateException();
            if (counts.containsKey(e.phrase)) {
                counts.put(e.phrase, counts.get(e.phrase) + e.count);
            } else {
                counts.put(e.phrase, e.count);
            }
        }
        PrunedCounts<String> pruned = pruner.prune(counts);
        if (pruned != null) {
            phraseDao.savePageCounts(lang, wpId, pruned);
        }
    }

    protected void writePhrase(List<Entry> pageCounts, PrunedCounts.Pruner pruner) throws DaoException {
        if (pageCounts.isEmpty()) {
            return;
        }
        Language lang = pageCounts.get(0).language;
        String phrase = normalize(lang, pageCounts.get(0).phrase);
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for (Entry e : pageCounts) {
            if (!normalize(lang, e.phrase).equals(phrase)) {
                LOG.warn("disagreement between phrases " + phrase + " and " + e.phrase);
            }
            if (e.language != lang) {
                LOG.warn("disagreement between languages " + lang+ " and " + e.language);
            }
            if (counts.containsKey(e.localId)) {
                counts.put(e.localId, counts.get(e.localId) + e.count);
            } else {
                counts.put(e.localId, e.count);
            }
        }
        PrunedCounts<Integer> pruned = pruner.prune(counts);
        if (pruned != null) {
            phraseDao.savePhraseCounts(lang, phrase, pruned);
        }
    }

    private void sortInPlace(File file) throws IOException {
        int maxFiles = Math.max(100, (int) (file.length() / (Runtime.getRuntime().maxMemory() / 20)));
        LOG.info("sorting " + file + " using max of " + maxFiles);
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                return r1.compareTo(r2);}};
        List<File> l = ExternalSort.sortInBatch(file, comparator, maxFiles, Charset.forName("utf-8"), null, false);
        LOG.info("merging " + file);
        ExternalSort.mergeSortedFiles(l, file, comparator, Charset.forName("utf-8"));
        LOG.info("finished sorting" + file);
    }


    @Override
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException {
        LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
        PrunedCounts<String> counts = phraseDao.getPageCounts(language, page.getLocalId(), maxPhrases);
        if (counts == null) {
            return null;
        }
        for (String phrase : counts.keySet()) {
            result.put(phrase, (float)1.0 * counts.get(phrase) / counts.getTotal());
            if (result.size() >= maxPhrases) {
                break;
            }
        }
        return result;
    }

    @Override
    public LinkedHashMap<LocalId, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalId, Float> result = new LinkedHashMap<LocalId, Float>();
        PrunedCounts<Integer> counts = phraseDao.getPhraseCounts(language, phrase, maxPages);
        if (counts == null) {
            return null;
        }
        for (Integer wpId : counts.keySet()) {
            result.put(new LocalId(language, wpId),
                    (float)1.0 * counts.get(wpId) / counts.getTotal());
            if (result.size() >= maxPages) {
                break;
            }
        }
        return result;
    }

    public PhraseAnalyzerDao getDao() {
        return phraseDao;
    }

}
