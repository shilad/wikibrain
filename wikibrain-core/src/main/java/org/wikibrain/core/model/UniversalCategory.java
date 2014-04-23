package org.wikibrain.core.model;

import com.google.common.collect.Multimap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

/**
 */
public class UniversalCategory extends UniversalPage{
    public UniversalCategory(int univId, int algorithmId, Multimap<Language, LocalId> localPages) {
        super(univId, algorithmId, NameSpace.CATEGORY, localPages);
    }
}
