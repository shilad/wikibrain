package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

/**
 */
public class UniversalCategory extends UniversalPage<LocalCategory>{
    public UniversalCategory(int univId, int algorithmId, Multimap<Language, LocalCategory> localPages) {
        super(univId, algorithmId, NameSpace.CATEGORY, localPages);
    }
}
