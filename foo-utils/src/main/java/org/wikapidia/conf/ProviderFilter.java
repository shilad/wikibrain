package org.wikapidia.conf;

import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;

/**
 * SubclassFilter should be able to do this, but it doesn't for me.
 * Note that inherited providers WILL NOT work because of this crappy
 * implementation.
 */
public class ProviderFilter implements ClassFilter {

    @Override
    public boolean accept(ClassInfo classInfo, ClassFinder classFinder) {
        return classInfo.getClass() != null && classInfo.getSuperClassName() != null &&
        classInfo.getSuperClassName().equals(Provider.class.getName());
    }
}
