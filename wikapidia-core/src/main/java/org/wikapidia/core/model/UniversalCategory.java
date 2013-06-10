package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

/**
 * Created with IntelliJ IDEA.
 * User: research
 * Date: 6/7/13
 * Time: 4:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UniversalCategory extends UniversalPage<LocalCategory>{
    public UniversalCategory(int univId, Multimap<Language, LocalCategory> localPages) {
        super(univId, localPages);
    }
}
