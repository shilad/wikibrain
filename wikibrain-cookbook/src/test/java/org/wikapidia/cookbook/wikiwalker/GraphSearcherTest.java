package org.wikapidia.cookbook.wikiwalker;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.core.model.LocalPage;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
@Ignore
public class GraphSearcherTest {
    private static WikAPIdiaWrapper wrapper;
    private static LocalPage obama;
    private static LocalPage apple;
    private static LocalPage mondale;
    private static LocalPage bayes;
    private static LocalPage coltrane;
    private static LocalPage norse;
    private static LocalPage loki;


    @BeforeClass
    public static void setup() {
        wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        apple = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Apple");
        mondale = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Walter Mondale");
        bayes = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Bayes' theorem");
        coltrane = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "John Coltrane");
        norse = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Norse mythology");
        loki = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Loki");
        for (LocalPage page : Arrays.asList(obama, apple, mondale, coltrane, norse, loki)) {
            wrapper.setInteresting(Utils.LANG_SIMPLE, page.getLocalId(), true);
        }
    }

    @Test
    public void testIdentity() throws Exception {
        GraphSearcher searcher = new GraphSearcher(wrapper);
        assertEquals(0, searcher.shortestDistance(apple, apple));
        assertEquals(1, searcher.shortestPath(apple, apple).size());
    }

    @Test
    public void testDisconnected() throws Exception {
        GraphSearcher searcher = new GraphSearcher(wrapper);
        assertEquals(null, searcher.shortestPath(bayes, coltrane));
    }

    @Test
    public void testClose() throws Exception {
        GraphSearcher searcher = new GraphSearcher(wrapper);

        // Apple -> norse
        assertEquals(1, searcher.shortestDistance(apple, norse));
        assertEquals(2, searcher.shortestPath(apple, norse).size());

        // Apple -> norse -> liki
        assertEquals(2, searcher.shortestDistance(apple, loki));
        assertEquals(3, searcher.shortestPath(apple, loki).size());
    }

    @Test
    public void testFar() throws Exception {
        GraphSearcher searcher = new GraphSearcher(wrapper);

        List<LocalPage> path = searcher.shortestPath(obama, apple);
        assertEquals(path.size(), 5);
        assertEquals(path.get(0).getTitle().getCanonicalTitle(), "Barack Obama");
        assertEquals(path.get(3).getTitle().getCanonicalTitle(), "Group");
        assertEquals(path.get(4).getTitle().getCanonicalTitle(), "Apple");

        // Obama -> Inauguration -> Audience -> Group -> Apple
        assertEquals(4, searcher.shortestDistance(obama, apple));

        // Mondale -> Rockefeller -> Family -> Group -> Apple
        assertEquals(4, searcher.shortestDistance(mondale, apple));

    }
}
