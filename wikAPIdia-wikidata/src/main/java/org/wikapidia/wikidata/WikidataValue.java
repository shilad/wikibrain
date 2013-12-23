package org.wikapidia.wikidata;

/**
 * Created by bjhecht on 12/23/13.
 */
public class WikidataValue {

    public enum WikidataValueType {ITEM, OTHER};

    private WikidataValueType type;
    private Object value;

}
