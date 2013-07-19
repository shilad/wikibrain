package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;

/**
 */
public class UniversalCategory extends UniversalPage{
    public UniversalCategory(int univId, int algorithmId, Multimap<Language, LocalId> localPages) {
        super(univId, algorithmId, NameSpace.CATEGORY, localPages);
    }
}
