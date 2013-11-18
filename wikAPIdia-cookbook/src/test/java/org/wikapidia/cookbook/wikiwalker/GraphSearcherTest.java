package org.wikapidia.cookbook.wikiwalker;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.core.model.LocalPage;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class GraphSearcherTest {
    private static WikAPIdiaWrapper wrapper;
    private static LocalPage obama;
    private static LocalPage apple;
    private static LocalPage mondale;

    @BeforeClass
    public static void setup() {
        wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        apple = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Apple");
        mondale = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Walter Mondale");
    }

    @Test
    public void testIdentity() throws Exception {
        GraphSearcher searcher = new GraphSearcher();
        assertEquals(0, searcher.shortestDistance(apple, apple));
    }

    @Test
    public void testClose() throws Exception {
        GraphSearcher searcher = new GraphSearcher();

        // Obama -> President of the US -> Mondale
        assertEquals(2, searcher.shortestDistance(obama, mondale));
    }

    @Test
    public void testFar() throws Exception {
        GraphSearcher searcher = new GraphSearcher();

        // Obama -> African-American people -> Skin -> Apple
        assertEquals(3, searcher.shortestDistance(obama, apple));

        // Mondale -> List of vice presidents -> Maine -> Apple
        assertEquals(3, searcher.shortestDistance(mondale, apple));
    }
}
