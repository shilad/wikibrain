package org.wikapidia.phrases.stanford;

import com.sleepycat.je.DatabaseException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PageDescriber;
import org.wikapidia.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * A page describer that uses the Stanford Wikipedia links dataset.
 */
public class StanfordPageDescriber implements PageDescriber {
    private final LocalPageDao dao;
    ObjectDb<StoredPageRecord> db;

    public StanfordPageDescriber(File path, boolean isNew, LocalPageDao dao) throws IOException, DatabaseException {
        this.db = new ObjectDb<StoredPageRecord>(path, isNew);
        this.dao = dao;
    }

    public void add(InputRecord record) throws DaoException, DatabaseException, IOException, ClassNotFoundException {
        LanguageInfo en = LanguageInfo.getByLangCode("en");
        LocalPage page = dao.getByTitle(
                    en.getLanguage(),
                    new Title(record.getArticle(), en),
                    NameSpace.ARTICLE
                );
        StoredPageRecord value = db.get(page.getTitle().getCanonicalTitle());
        if (value == null) {
            value = new StoredPageRecord(page.getLocalId());
        }
        value.add(record.getText(), record.getNumberEnglishLinks());
        db.put("" + page.getLocalId(), value);
    }

    public void prune(int minCount, int maxRank, double minFrac) {

    }

    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) {
        if (!language.getLangCode().equals("en")) {
            throw new UnsupportedOperationException("StanfordPageDescriber only supports English");
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        if (!language.getLangCode().equals("en")) {
            throw new UnsupportedOperationException("StanfordPageDescriber only supports English");
        }
        return null;
    }
}
