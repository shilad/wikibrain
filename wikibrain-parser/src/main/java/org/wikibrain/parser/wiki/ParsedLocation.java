package org.wikibrain.parser.wiki;

import org.wikibrain.core.model.RawPage;

public class ParsedLocation {
    /**
     * Enclosing page.
     */
    private RawPage xml;

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

    public ParsedLocation(RawPage xml, int section, int paragraph, int location) {
        this.xml = xml;
        this.section = section;
        this.paragraph = paragraph;
        this.location = location;
    }

    public RawPage getXml() {
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
