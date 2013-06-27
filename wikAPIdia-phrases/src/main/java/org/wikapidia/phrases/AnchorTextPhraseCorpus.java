//package org.wikapidia.phrases;
//
//import org.wikapidia.core.dao.DaoException;
//import org.wikapidia.core.dao.DaoFilter;
//import org.wikapidia.core.dao.LocalLinkDao;
//import org.wikapidia.core.lang.LanguageInfo;
//import org.wikapidia.core.lang.LanguageSet;
//import org.wikapidia.core.model.LocalLink;
//import org.wikapidia.phrases.PhraseAnalyzerDao;
//
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Loads phrase to page mapping using anchor text in wiki links.
// */
//public class AnchorTextPhraseCorpus implements PhraseCorpus {
//    private static final Logger LOG = Logger.getLogger(AnchorTextPhraseCorpus.class.getName());
//    private static final LanguageInfo EN = LanguageInfo.getByLangCode("en");
//    private final LanguageSet langs;
//
//    private LocalLinkDao linkDao;
//
//    public AnchorTextPhraseCorpus(PhraseAnalyzerDao phraseDao, LocalLinkDao linkDao, LanguageSet langs) {
//        this.linkDao = linkDao;
//        this.langs = langs;
//    }
//
//    /**
//     * Loads language links into the database.
//     * TODO: optimize this by doing some counting in memory if necessary.
//     * @throws java.io.IOException
//     */
//    public void loadCorpus(PhraseAnalyzerDao dao) throws IOException, DaoException {
//        long numLines = 0;
//        long numLinesRetained = 0;
//        for (LocalLink ll : linkDao.get(new DaoFilter().setLanguages(langs))) {
//            if (++numLines % 100000 == 0) {
//                double p = 100.0 * numLinesRetained / numLines;
//                LOG.info("processing line: " + numLines +
//                        ", retained " + numLinesRetained +
//                        "(" + new DecimalFormat("#.#").format(p) + "%)");
//            }
//            try {
//                dao.add(ll.getLanguage(), ll.getDestId(), ll.getAnchorText(), 1);
//                numLinesRetained++;
//            } catch (Exception e) {
//                LOG.log(Level.FINEST, "Error parsing link " + ll + ":", e);
//            }
//        }
//    }
//}
