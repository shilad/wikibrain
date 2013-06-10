package org.wikapidia.conf;

import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;

import static org.junit.Assert.*;

public class TestConfigurator {
    @Test
    public void testSimple() throws ConfigurationException {
        // Should pick up configuration in reference.conf
        Configuration conf = new Configuration();
        Configurator confer = new Configurator(conf);
        Integer i = (Integer) confer.get(Integer.class, "foo");
        assertEquals(i, 42);
        Integer j = (Integer) confer.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = (Integer) confer.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = (Integer) confer.get(Integer.class, "biff");
        assertEquals(l, 1);
    }
}
