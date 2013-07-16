package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Brent Hecht
 */
public class UniversalPage<T extends LocalPage> extends AbstractUniversalEntity<T> {

    /**
     * The universal id for the universal page. Universal ids are defined within but not across namespaces.
     */
    private final int univId;
    private final NameSpace nameSpace;

    public UniversalPage(int univId, int algorithmId, NameSpace nameSpace, Multimap<Language, T> localPages) {
        super(algorithmId, localPages);
        this.univId = univId;
        this.nameSpace = nameSpace;
    }

    public UniversalPage(int univId, int algorithmId, NameSpace nameSpace, LanguageSet languages) {
        super(algorithmId, languages);
        this.univId = univId;
        this.nameSpace = nameSpace;
    }

    public int getUnivId(){
        return univId;
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public Collection<T> getLocalPages(Language language) {
        return new ArrayList<T>(getLocalEntities(language));
    }

    public static interface LocalPageChooser<T extends LocalPage> {
        public T choose(Collection<T> localPages);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UniversalPage) {
            UniversalPage other = (UniversalPage) o;
            return (this.getUnivId() == other.getUnivId() &&
                    this.getAlgorithmId() == other.getAlgorithmId());
        } else {
            return false;
        }
    }
}
