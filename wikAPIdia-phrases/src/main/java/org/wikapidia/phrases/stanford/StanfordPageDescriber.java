package org.wikapidia.phrases.stanford;

import com.sleepycat.je.DatabaseException;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PageDescriber;
import org.wikapidia.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * A generic page describer.
 */
public class StanfordPageDescriber implements PageDescriber {
    private final LocalPageDao dao;
    ObjectDb<StoredPageRecord> db;

    public StanfordPageDescriber(File path, boolean isNew, LocalPageDao dao) throws IOException, DatabaseException {
        this.db = new ObjectDb<StoredPageRecord>(path, isNew);
        this.dao = dao;
    }

    public void add(LocalPage page, String phrase, int count) throws DaoException {
        add(page.getLanguage(), page.getLocalId(), phrase, count);
    }

    public void add(Language lang, int wpId, String phrase, int count) throws DaoException {
        String key = lang.getLangCode() + wpId;
        StoredPageRecord value = null;
        try {
            value = db.get(key);
            if (value == null) {
                value = new StoredPageRecord(wpId);
            }
            value.add(phrase, count);
            db.put(key, value);
        } catch (DatabaseException e) {
            throw new DaoException(e);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    public void freezeAndPrune(int minCount, int maxRank, double minFrac) throws DaoException {
        for (Pair<String, StoredPageRecord> entry : db) {
            StoredPageRecord record = entry.getValue();
            record.freeze();
            // update r to be correct maxRank for this entry
            int sum = record.sumCounts();
            int i = 0;
            for (; i < record.numPhrases() && i < maxRank; i++) {
                int c = record.getCount(i);
                if (c < minCount || 1.0 * c / sum < minFrac) {
                    break;
                }
            }
            if (i < record.numPhrases()) {
                record.prune(i);
            }
            try {
                db.put(entry.getKey(), record);
            } catch (DatabaseException e) {
                throw new DaoException(e);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }
    }

    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException {
        if (!language.getLangCode().equals("en")) {
            throw new UnsupportedOperationException("StanfordPageDescriber only supports English");
        }
        try {
            StoredPageRecord record = db.get(language.getLangCode() + page.getLocalId());
            if (record == null) {
                return null;
            }
            int sum = record.sumCounts();
            LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
            for (int i = 0; i < record.numPhrases() && i < maxPhrases; i++) {
                result.put(record.getPhrase(i), (float)(1.0 * record.getCount(i) / sum));
            }
            return result;
        } catch (DatabaseException e) {
            throw new DaoException(e);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        if (!language.getLangCode().equals("en")) {
            throw new UnsupportedOperationException("StanfordPageDescriber only supports English");
        }
        // TODO: figure out what to do!
        throw new UnsupportedOperationException();
    }
}
