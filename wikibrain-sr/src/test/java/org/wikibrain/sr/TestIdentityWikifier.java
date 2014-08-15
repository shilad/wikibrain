package org.wikibrain.sr;

import org.junit.Test;
import org.mockito.Mockito;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.sr.wikify.IdentityWikifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestIdentityWikifier {
    private static final LocalLink.LocationType LOC_TYPE = LocalLink.LocationType.FIRST_PARA;
    private static final String TEXT1 =
            "North Battleford is a city in the Canadian province of Saskatchewan. " +
            "The population of North Battleford was 13,190 in 2006[1]. " +
            "The mayor of North Battleford is Ian Hamilton. " +
            "The Battlefords area was home to several aboriginal groups including Cree, " +
            "Assiniboine and Blackfoot tribes who contested for control of local " +
            "resources before the European settlement.";

    @Test
    public void testOne() throws DaoException {
        List<LocalLink> result = wikify(TEXT1,
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 1, true, LOC_TYPE)
        );
        assertEquals(1, result.size());
        LocalLink ll = result.get(0);
        assertEquals(0, ll.getLocation());
        assertEquals(12, ll.getSourceId());
        assertEquals(4, ll.getDestId());
        assertEquals("North Battleford", ll.getAnchorText());
    }


    @Test
    public void testConsecutive() throws DaoException {
        List<LocalLink> result = wikify(TEXT1,
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 1, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "Ian Hamilton", 12, 4, true, 8, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 3, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "European settlement.", 12, 4, true, 10, true, LOC_TYPE)
        );
        assertEquals(4, result.size());
        assertEquals("North Battleford", result.get(0).getAnchorText());
        assertEquals("North Battleford", result.get(1).getAnchorText());
        assertEquals("Ian Hamilton", result.get(2).getAnchorText());
        assertEquals("European settlement.", result.get(3).getAnchorText());
        for (LocalLink ll : result) {
            System.err.println("ll is " + ll);
            assertEquals(ll.getAnchorText(), TEXT1.substring(ll.getLocation(), ll.getLocation() + ll.getAnchorText().length()));
        }
    }

    @Test
    public void testConsecutiveMissingOne() throws DaoException {
        List<LocalLink> result = wikify(TEXT1,
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 1, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "Ian Hamilton", 12, 4, true, 8, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 3, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "ZZZZ", 12, 4, true, 3, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "European settlement.", 12, 4, true, 10, true, LOC_TYPE)
        );
        assertEquals(4, result.size());
        assertEquals("North Battleford", result.get(0).getAnchorText());
        assertEquals("North Battleford", result.get(1).getAnchorText());
        assertEquals("Ian Hamilton", result.get(2).getAnchorText());
        assertEquals("European settlement.", result.get(3).getAnchorText());
        for (LocalLink ll : result) {
            System.err.println("ll is " + ll);
            assertEquals(ll.getAnchorText(), TEXT1.substring(ll.getLocation(), ll.getLocation() + ll.getAnchorText().length()));
        }
    }

    @Test
    public void testComplicated() throws DaoException {
        List<LocalLink> result = wikify(TEXT1,
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 1, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "Ian Hamilton", 12, 4, true, 8, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "FFF", 12, 4, true, 8, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "North Battleford", 12, 4, true, 3, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "ZZZZ", 12, 4, true, 3, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "European settlement.", 12, 4, true, 10, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, "Assiniboine", 12, 4, true, 11, true, LOC_TYPE),
                new LocalLink(Language.SIMPLE, ", Assin", 12, 4, true, 12, true, LOC_TYPE)
        );
        assertEquals(5, result.size());
        assertEquals("North Battleford", result.get(0).getAnchorText());
        assertEquals("North Battleford", result.get(1).getAnchorText());
        assertEquals("Ian Hamilton", result.get(2).getAnchorText());
        assertEquals("Assiniboine", result.get(3).getAnchorText());
        assertEquals("European settlement.", result.get(4).getAnchorText());
        for (LocalLink ll : result) {
            System.err.println("ll is " + ll);
            assertEquals(ll.getAnchorText(), TEXT1.substring(ll.getLocation(), ll.getLocation() + ll.getAnchorText().length()));
        }
    }

    private List<LocalLink> wikify(String text, LocalLink ... links) throws DaoException {
        LocalLinkDao llDao = Mockito.mock(LocalLinkDao.class);
        Mockito.when(llDao.getLinks(Language.SIMPLE, 12, true)).thenReturn(Arrays.asList(links));
        IdentityWikifier wikifier = new IdentityWikifier(Language.SIMPLE, null, llDao);
        return wikifier.wikify(12, text);
    }
}
