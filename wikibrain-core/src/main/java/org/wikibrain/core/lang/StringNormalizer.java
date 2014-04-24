package org.wikibrain.core.lang;

/**
 * @author Shilad Sen
 */
public interface StringNormalizer {

    /**
     * Normalizes a string into a canonical representation.
     * @param language language of the passed in text
     * @param text
     * @return canonical representation.
     */
    public String normalize(Language language, String text);


    /**
     * Normalizes a string into a canonical representation.
     * @param text
     * @return canonical representation.
     */
    public String normalize(LocalString text);
}
