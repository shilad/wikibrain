package org.wikibrain.parser.wiki;

import org.wikibrain.core.model.Title;

public class ParsedLink extends ParsedEntity {
    public static enum SubarticleType {
        MAIN_TEMPLATE,
        SEEALSO_TEMPLATE,
        MAIN_INLINE,
        SEEALSO_INLINE,
        SEEALSO_HEADER
    }

    // Destination of link
    public Title target;

    // Anchor text
    public String text;

    // Null indicates no subarticle
    public SubarticleType subarticleType = null;
}
