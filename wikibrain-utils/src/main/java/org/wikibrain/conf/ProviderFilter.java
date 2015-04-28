package org.wikibrain.conf;

import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;

import java.lang.reflect.Modifier;

/**
 * SubclassFilter should be able to do this, but it doesn't for me.
 * Note that inherited providers WILL NOT work because of this crappy
 * implementation.
 */
public class ProviderFilter implements ClassFilter {

    @Override
    public boolean accept(ClassInfo classInfo, ClassFinder classFinder) {
        if (classInfo.getClass() == null || classInfo.getSuperClassName() == null) {
            return false;
        }
        // Support two-level hierarchy
        if (!classInfo.getSuperClassName().equals(Provider.class.getName())) {
            return false;
        }

        return !Modifier.isInterface(classInfo.getModifier()) && !Modifier.isAbstract(classInfo.getModifier());
    }
}
