package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

/**
 * Created with IntelliJ IDEA.
 * User: Brent Hecht
 */
public class UniversalArticle extends UniversalPage<LocalArticle>{

    public UniversalArticle(int univId, Multimap<Language, LocalArticle> localPages) {
        super(univId, PageType.ARTICLE, localPages);
    }

}
