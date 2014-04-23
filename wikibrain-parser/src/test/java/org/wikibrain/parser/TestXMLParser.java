package org.wikibrain.parser;

import org.junit.Test;
import static org.junit.Assert.*;

import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.xml.PageXmlParser;

/**
 */
public class TestXMLParser {

    @Test
    public void test() throws WpParseException{
        String rawww = "<page>\n" +
                "    <title>Wikipedia:Featured article candidates</title>\n" +
                "    <ns>4</ns>\n" +
                "    <id>79</id>\n" +
                "    <redirect title=\"Wikipedia:Proposed very good articles\" />\n" +
                "    <revision>\n" +
                "      <id>1136669</id>\n" +
                "      <parentid>1128223</parentid>\n" +
                "      <timestamp>2008-10-28T19:16:17Z</timestamp>\n" +
                "      <contributor>\n" +
                "        <username>American Eagle</username>\n" +
                "        <id>16647</id>\n" +
                "      </contributor>\n" +
                "      <comment>redirect to [[Wikipedia:Proposed very good articles]] per [[Wikipedia:Requests for deletion/Requests/2008/Wikipedia:Featured article candidates]]</comment>\n" +
                "      <text xml:space=\"preserve\">#REDIRECT [[Wikipedia:Proposed very good articles]]</text>\n" +
                "      <sha1>iipb0jon72fukl9ia2u9pqg7siw5n4c</sha1>\n" +
                "      <model>wikitext</model>\n" +
                "      <format>text/x-wiki</format>\n" +
                "    </revision>\n" +
                "  </page>";

        LanguageInfo lang = LanguageInfo.getByLangCode("simple");
        PageXmlParser parser = new PageXmlParser(lang);
        RawPage rawwwPage = parser.parse(rawww, 0, rawww.length());

        assert (rawwwPage.getTitle().getCanonicalTitle().equals("Wikipedia:Featured article candidates"));
        assert (rawwwPage.isRedirect());
        assertEquals(rawwwPage.getNamespace(), NameSpace.WIKIPEDIA);  //TODO: Test this one out.
    }

}
