package org.wikibrain.utils;

import com.sleepycat.je.DatabaseException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class TestObjectDb {

    @Test
    public void testPutGet() throws ConfigurationException, IOException, DatabaseException, ClassNotFoundException {
        ObjectDb<Integer> db = getObjectDb();
        assertNull(db.get("foo"));
        db.put("foo", 324);
        assertEquals(db.get("foo"), 324);
        db.put("bar", 11);
        assertEquals(db.get("bar"), 11);
        db.put("foo", 24);
        assertEquals(db.get("foo"), 24);
    }

    @Test
    public void testIterate() throws ConfigurationException, IOException, DatabaseException, ClassNotFoundException {
        ObjectDb<Integer> db = getObjectDb();
        db.put("foo", 324);
        db.put("bar", 11);
        db.put("foo", 24);
        db.put("asdfasdf", 24);
        db.put("zab", 26);
        db.remove("asdfasdf");


        Map<String, Integer> iteratedMap = new HashMap<String, Integer>();
        for (Pair<String, Integer> pair : db) {
            iteratedMap.put(pair.getKey(), pair.getValue());
        }
        Map<String, Integer> expectedMap =  new HashMap<String, Integer>();
        expectedMap.put("bar", 11);
        expectedMap.put("foo", 24);
        expectedMap.put("zab", 26);
        assertEquals(iteratedMap, expectedMap);
    }

    @Test
    public void testIterateRemove() throws ConfigurationException, IOException, DatabaseException, ClassNotFoundException {
        ObjectDb<Integer> db = getObjectDb();
        db.put("foo", 324);
        db.put("bar", 11);
        db.put("baz", 24);


        int i = 0;
        for (Pair<String, Integer> pair : db) { i++; }
        assertEquals(i, 3);
        i = 0;
        Iterator<Pair<String, Integer>> iter = db.iterator();
        while (iter.hasNext()) {
            Pair<String, Integer> p = iter.next();
            if (p.getKey().equals("foo")) iter.remove();
        }
        i = 0;
        for (Pair<String, Integer> pair : db) { i++; }
        assertEquals(i, 2);
        assertNull(db.get("foo"));
    }

    private ObjectDb getObjectDb() throws IOException, DatabaseException {
        File tmp = File.createTempFile("testdb", ".db", null);
        tmp.deleteOnExit();
        tmp.delete();
        return new ObjectDb(tmp, true);
    }
}
