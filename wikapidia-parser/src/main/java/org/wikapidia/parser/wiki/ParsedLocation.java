package org.wikapidia.parser.wiki;

import org.wikapidia.parser.xml.PageXml;

public class ParsedLocation {
    /**
     * Enclosing page.
     */
    private PageXml xml;

    /**
     * Section 0 is the first section
     */
    private int section;

    /**
     *
     * Paragraph numbers before first paragraph are negative
     * paragraph 0 is the first paragraph.
     */
    private int paragraph;

    /**
     * Location in bytes of the element.
     */
    private int location;

    public ParsedLocation(PageXml xml, int section, int paragraph, int location) {
        this.xml = xml;
        this.section = section;
        this.paragraph = paragraph;
        this.location = location;
    }

    public PageXml getXml() {
        return xml;
    }

    public int getSection() {
        return section;
    }

    public int getParagraph() {
        return paragraph;
    }

    public int getLocation() {
        return location;
    }
}
