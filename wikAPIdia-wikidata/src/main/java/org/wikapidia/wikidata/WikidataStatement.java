package org.wikapidia.wikidata;

/**
 * Created by bjhecht on 12/23/13.
 */
public class WikidataStatement {

    public static enum Rank {DEPRECATED, NORMAL, PREFERRED};

    private String uuid;
    private WikidataItem item;
    private WikidataProperty property;
    private WikidataValue value;
    private Rank rank;


}
