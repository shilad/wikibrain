package org.wikapidia.phrases;

import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.phrases.dao.PhraseAnalyzerDao;
import org.wikapidia.utils.WpIOUtils;

import java.io.File;

/**
 * Loads phrase to page files from Indexes files from
 * http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/
 * into a PhraseAnalyzer
 *
 * These files capture anchor text associated with web pages that link to Wikipedia.
 * Note that the pages with anchor text are not (usually) Wikipedia pages themselves.
 */
public class StanfordPhraseLoader {
    private PhraseAnalyzerDao phraseDao;
    private LocalPageDao pageDao;

    public StanfordPhraseLoader(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao) {
        this.phraseDao = phraseDao;
        this.pageDao = pageDao;
    }

    public void load(File path) {
    }
}
