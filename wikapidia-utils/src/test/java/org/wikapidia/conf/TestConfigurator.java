package org.wikapidia.conf;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class TestConfigurator {
    @Test
    public void testSimple() throws ConfigurationException {
        // Should pick up configuration in reference.conf
        Configurator conf = new Configurator(new Configuration());
        Integer i = (Integer) conf.get(Integer.class, "foo");
        assertEquals(i, 42);
        Integer j = (Integer) conf.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = (Integer) conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = (Integer) conf.get(Integer.class, "biff");
        assertEquals(l, 1);
    }

    @Test
    public void testSpecificFile() throws ConfigurationException, IOException {
        File tmp = File.createTempFile("myconf", ".conf", null);
        tmp.deleteOnExit();
        FileUtils.write(tmp,
                "providers : { some.path.intMaker += org.wikapidia.conf.OddIntProvider }\n" +
                "some.path.intMaker : { aaa : { type : odd } }\n" +
                "some.path.intMaker : { bbb : { type : odd } }\n"
            );
        Configurator conf = new Configurator(new Configuration(tmp));

        Integer i = (Integer) conf.get(Integer.class, "foo");
        assertEquals(i, 42);
        Integer j = (Integer) conf.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = (Integer) conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = (Integer) conf.get(Integer.class, "biff");
        assertEquals(l, 1);

        Integer m = (Integer) conf.get(Integer.class, "aaa");
        assertEquals(m, 1);
        Integer n = (Integer) conf.get(Integer.class, "bbb");
        assertEquals(n, 3);

        tmp.delete();
    }
}
