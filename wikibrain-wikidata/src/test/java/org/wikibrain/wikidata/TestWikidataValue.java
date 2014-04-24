package org.wikibrain.wikidata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.wikibrain.utils.WpIOUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestWikidataValue {

    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        Object value = Arrays.asList("Foo", "bar", "baz");
        JsonElement json = new JsonParser().parse("['Foo', 'bar', 'baz']");
        WikidataValue wdVal = new WikidataValue("atype", value, json);
        byte[] bytes = WpIOUtils.objectToBytes(wdVal);
        WikidataValue wdVal2 = (WikidataValue) WpIOUtils.bytesToObject(bytes);
        byte[] bytes2 = WpIOUtils.objectToBytes(wdVal2);

        assertEquals(bytes.length, bytes2.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], bytes2[i]);
        }
    }
}
