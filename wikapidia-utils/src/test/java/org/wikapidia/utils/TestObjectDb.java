package org.wikapidia.utils;

import com.sleepycat.je.DatabaseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TestObjectDb {

    @Test
    public void testPutGet() throws ConfigurationException, IOException, DatabaseException, ClassNotFoundException {
        ObjectDb<Integer> db = getObjectDb();
        assertNull(db.get("foo"));
        db.put("foo", new Integer(324));
        assertEquals(db.get("foo"), 324);
        db.put("bar", new Integer(11));
        assertEquals(db.get("bar"), 11);
        db.put("foo", new Integer(24));
        assertEquals(db.get("foo"), 24);
    }

    @Test
    public void testIterate() throws ConfigurationException, IOException, DatabaseException, ClassNotFoundException {
        ObjectDb<Integer> db = getObjectDb();
        db.put("foo", new Integer(324));
        db.put("bar", new Integer(11));
        db.put("foo", new Integer(24));
        db.put("zab", new Integer(26));


        Map<String, Integer> iteratedMap = new HashMap<String, Integer>();

        for (Pair<String, Integer> pair : db) {
            System.out.println("key is " + pair.getKey());
            System.out.println("val is " + pair.getValue());
        }
    }

    private ObjectDb getObjectDb() throws IOException, DatabaseException {
        File tmp = File.createTempFile("testdb", ".db", null);
        tmp.deleteOnExit();
        tmp.delete();
        tmp.mkdir();
        return new ObjectDb(tmp, true);
    }
}
