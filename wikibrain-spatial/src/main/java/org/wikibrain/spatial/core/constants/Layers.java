package org.wikibrain.spatial.core.constants;

/**
 *
 * Series of static variables to simplify access to layers loaded by default.
 *
 */
public class Layers {

    public static String WIKIDATA = "wikidata";
    public static String GADM0 = "gadm0";
    public static String GADM1 = "gamd1";

    /**
     * Gets a layer name for a type-based Wikidata layer.
     * @param type
     * @return
     */
    public static String wikidata(String type){
        return WIKIDATA + "_" + type;
    }

}
