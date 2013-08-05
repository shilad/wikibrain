package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.lucene.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Using Lucene in a phrase analyzer.
 * @author Yulun Li
 */
public class LucenePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(PhraseAnalyzer.class.getName());
    private final LuceneSearcher searcher;

    protected LocalPageDao localPageDao;

    public LucenePhraseAnalyzer(LocalPageDao localPageDao, LuceneSearcher searcher) {
        this.localPageDao = localPageDao;
        this.searcher = searcher;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language, searcher.getOptions());
//        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.search(queryBuilder.getPhraseQuery(new TextFieldElements().addTitle(), phrase), language, 10);
        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.search(queryBuilder.getPhraseQuery(new TextFieldElements().addPlainText(), phrase), language, 10);
        if (wikapidiaScoreDocs.length == 0 && phrase.indexOf(" ") < 0 && phrase.length() > 3) {
            String phraseMultiVersion = "";
            for (int i = 1; i < phrase.length(); i++) {
                phraseMultiVersion += (i > 2 ? phrase.substring(0, i) + " " : "");
                phraseMultiVersion += (phrase.length() - i > 2 ? phrase.substring(i, phrase.length()) + " " : "");
            }
            wikapidiaScoreDocs = searcher.search(queryBuilder.getPhraseQuery(new TextFieldElements().addPlainText(), phraseMultiVersion), language, 10);
        }

//        if (wikapidiaScoreDocs.length != 0) {
//            if (wikapidiaScoreDocs1.length !=0) {
//                if (wikapidiaScoreDocs[0].doc != wikapidiaScoreDocs1[0].doc) {
//                    System.out.println(phrase);
//                    System.out.println("Using title:" + localPageDao.getById(language, searcher.getLocalIdFromDocId(wikapidiaScoreDocs[0].doc, language)).getTitle().getCanonicalTitle() + " Score:" + wikapidiaScoreDocs[0].score);
//                    System.out.println("Using title:" + localPageDao.getById(language, searcher.getLocalIdFromDocId(wikapidiaScoreDocs1[0].doc, language)).getTitle().getCanonicalTitle() + " Score:" + wikapidiaScoreDocs1[0].score);
//                }
//            } else {
//                System.out.println(phrase);
//                System.out.println("Plainttext!! " + localPageDao.getById(language, searcher.getLocalIdFromDocId(wikapidiaScoreDocs1[0].doc, language)).getTitle().getCanonicalTitle() + " Score:" + wikapidiaScoreDocs1[0].score);
//            }
//        } else {
//            if (wikapidiaScoreDocs1.length !=0) {
//                System.out.println(phrase);
//                System.out.println("Title!! " + localPageDao.getById(language, searcher.getLocalIdFromDocId1(wikapidiaScoreDocs[0].doc, language)).getTitle().getCanonicalTitle() + " Score:" + wikapidiaScoreDocs1[0].score);
//            } else {
//                System.out.println(phrase);
//                System.out.println("!!!!!");
//            }
//        }


        float totalScore = 0;
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            totalScore += wikapidiaScoreDoc.score;
        }
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.doc, language);
            LocalPage localPage = localPageDao.getById(language, localPageId);
            result.put(localPage, wikapidiaScoreDoc.score / totalScore);
        }
        return result;
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseAnalyzer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<PhraseAnalyzer> getType() {
            return PhraseAnalyzer.class;
        }

        @Override
        public String getPath() {
            return "phrases.analyzer";
        }
        @Override
        public PhraseAnalyzer get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("luceneAnalyzer")) {
                return null;
            }
            LocalPageDao localPageDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            LuceneSearcher searcher = new LuceneSearcher(
                    new LanguageSet(getConfig().get().getStringList("languages")),
                    getConfigurator().get(LuceneOptions.class));

            return new LucenePhraseAnalyzer(localPageDao, searcher);
        }

    }

    @Override
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages) {
        return null;
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        return null;
    }

    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException {
        return null;
    }

    @Override
    public void loadCorpus(LanguageSet langs) throws DaoException, IOException {
    }
}
