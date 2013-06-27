package org.wikapidia.phrases;

import com.google.code.externalsorting.ExternalSort;
import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzerDao;
import org.wikapidia.utils.WpIOUtils;
import org.wikapidia.utils.WpStringUtils;

import java.io.*;
import java.util.*;

/**
 * Simple implementation of a phrase analyzer.
 */
public abstract class SimplePhraseAnalyzer implements PhraseAnalyzer {

    /**
     * An entry in the corpus.
     */
    public static class Entry {
        Language language;
        // EITHER localId or title can identify to the article.
        int localId = -1;
        String title = null;
        String phrase;
        int count;

        private Entry(Language language, int localId, String phrase, int count) {
            this.language = language;
            this.localId = localId;
            this.phrase = phrase;
            this.count = count;
        }

        private Entry(Language language, String title, String phrase, int count) {
            this.language = language;
            this.title = title;
            this.phrase = phrase;
            this.count = count;
        }
    }

    protected PhraseAnalyzerDao phraseDao;
    protected LocalPageDao pageDao;

    public SimplePhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao) {
        this.phraseDao = phraseDao;
        this.pageDao = pageDao;
    }

    protected abstract Iterable<Entry> getCorpus() throws IOException, DaoException;

    /**
     * Loads a specific corpus into the dao.
     *
     * @throws DaoException
     * @throws IOException
     */
    @Override
    public void loadCorpus(PrunedCounts.Pruner<String> pagePruner, PrunedCounts.Pruner<Integer> phrasePruner) throws DaoException, IOException {
        File byWpIdFile = File.createTempFile("wp_phrases_by_id", "txt");
        byWpIdFile.deleteOnExit();
        BufferedWriter byWpId = WpIOUtils.openWriter(byWpIdFile);
        File byPhraseFile = File.createTempFile("wp_phrases_by_phrase", "txt");
        byPhraseFile.deleteOnExit();
        BufferedWriter byPhrase = WpIOUtils.openWriter(byPhraseFile);

        for (Entry e : getCorpus()) {
            if (e.localId < 0) {
                LocalPage lp = pageDao.getByTitle(e.language,
                        new Title(e.title, e.language),
                        NameSpace.ARTICLE);
                if (lp == null) {
                    continue;
                }
                e.localId = lp.getLocalId();
            }
            e.phrase.replace("\n", "");
            // phrase is last because it may contain tabs.
            String line = e.language.getLangCode() + "\t" + e.localId + "\t" + e.count + "\t" + e.phrase + "\n";
            byPhrase.write(e.language.getLangCode() + ":" + WpStringUtils.normalize(e.phrase) + "\t" + line);
            byWpId.write(e.language.getLangCode() + ":" + e.localId + "\t" + line);
        }

        byWpId.close();
        byPhrase.close();

        sortInPlace(byWpIdFile);
        sortInPlace(byPhraseFile);
        loadFromFile(RecordType.PAGES, byWpIdFile, pagePruner);
        loadFromFile(RecordType.PHRASES, byPhraseFile, phrasePruner);
    }

    private static enum RecordType {
        PAGES, PHRASES
    }

    protected void loadFromFile(RecordType ltype, File input, PrunedCounts.Pruner pruner) throws IOException, DaoException {
        BufferedReader reader = WpIOUtils.openReader(input);
        String lastKey = null;
        List<Entry> buffer = new ArrayList<Entry>();

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.split("\t", 5);

            // if new id, write out buffer and clear it
            if (lastKey != null && !tokens[0].equals(lastKey)) {
                if (ltype == RecordType.PAGES) writePage(buffer, pruner); else writePhrase(buffer, pruner);
                buffer.clear();
            }
            Entry e = new Entry(
                    Language.getByLangCode(tokens[1]),
                    new Integer(tokens[2]),
                    tokens[4],
                    new Integer(tokens[3])
            );
            buffer.add(e);
            lastKey = tokens[0];
        }
        if (ltype == RecordType.PAGES) writePage(buffer, pruner); else writePhrase(buffer, pruner);
    }

    protected void writePage(List<Entry> pageCounts, PrunedCounts.Pruner pruner) throws DaoException {
        if (pageCounts.isEmpty()) {
            return;
        }
        Language lang = pageCounts.get(0).language;
        int wpId = pageCounts.get(0).localId;
        Collections.sort(pageCounts, new Comparator<Entry>() {
            @Override
            public int compare(Entry e1, Entry e2) {
                return -1 * (e1.count - e2.count);
            }
        });
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Entry e : pageCounts) {
            if (e.localId != wpId) throw new IllegalStateException();
            if (e.language != lang) throw new IllegalStateException();
            counts.put(e.phrase, e.count);
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
        String phrase = WpStringUtils.normalize(pageCounts.get(0).phrase);
        Collections.sort(pageCounts, new Comparator<Entry>() {
            @Override
            public int compare(Entry e1, Entry e2) {
                return -1 * (e1.count - e2.count);
            }
        });
        LinkedHashMap<Integer, Integer> counts = new LinkedHashMap<Integer, Integer>();
        for (Entry e : pageCounts) {
            if (!WpStringUtils.normalize(e.phrase).equals(phrase)) throw new IllegalStateException();
            if (e.language != lang) throw new IllegalStateException();
            counts.put(e.localId, e.count);
        }
        PrunedCounts<Integer> pruned = pruner.prune(counts);
        if (pruned != null) {
            phraseDao.savePhraseCounts(lang, phrase, pruned);
        }
    }

    private void sortInPlace(File file) throws IOException {
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                return r1.compareTo(r2);}};
        List<File> l = ExternalSort.sortInBatch(file, comparator) ;
        ExternalSort.mergeSortedFiles(l, file, comparator);
    }


    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException {
        LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
        PrunedCounts<String> counts = phraseDao.getPageCounts(language, page.getLocalId(), maxPhrases);
        for (String phrase : counts.keySet()) {
            result.put(phrase, (float)1.0 * counts.get(phrase) / counts.getTotal());
            if (counts.size() >= maxPhrases) {
                break;
            }
        }
        return result;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();
        PrunedCounts<Integer> counts = phraseDao.getPhraseCounts(language, phrase, maxPages);
        for (Integer wpId : counts.keySet()) {
            result.put(pageDao.getById(language, wpId),
                    (float)1.0 * counts.get(wpId) / counts.getTotal());
            if (counts.size() >= maxPages) {
                break;
            }
        }
        return result;
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages) {
        throw new UnsupportedOperationException();
    }
}
