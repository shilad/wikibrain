package org.wikibrain.core.model;

import com.google.common.collect.Multimap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

/**
 * Created with IntelliJ IDEA.
 * User: Brent Hecht
 */
public class UniversalArticle extends UniversalPage{

    public UniversalArticle(int univId, int algorithmId, Multimap<Language, LocalId> localPages) {
        super(univId, algorithmId, NameSpace.ARTICLE, localPages);
    }

}
