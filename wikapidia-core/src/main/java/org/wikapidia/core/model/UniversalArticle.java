package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

/**
 * Created with IntelliJ IDEA.
 * User: Brent Hecht
 */
public class UniversalArticle extends UniversalPage<LocalArticle>{

    public UniversalArticle(int univId, int algorithmId, Multimap<Language, LocalArticle> localPages) {
        super(univId, algorithmId, NameSpace.ARTICLE, localPages);
    }

}
