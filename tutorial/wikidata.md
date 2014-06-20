---
    layout: default
    title: Wikidata
---
        

# Wikidata
Wikipedia's Wikidata initiative "aims to create a free knowledge base about the world that can be read and edited by humans and machines alike." In short, the initiative shifts the "facts" that support Wikipedia articles (Minneapolis is in Minnesota) into a structured universal repository that can be accessed in any language.

The WikiBrain library includes support for Wikidata. To use it, you must first import the Wikidata archive. This is a relatively large dataset (~10GB uncompressed), so it is not loaded by default during the regular pipeline. **AFTER running the regular pipeline** you need to do the following two steps.

First, Wikidata requires the "purewikidata" concept mapper. If you only installed one language (say *en* or *simple*, but not both) this will not have been installed. You will need to run:

```
./wb-java.sh org.wikibrain.dao.load.ConceptLoader -n purewikidata -d 
```

Next, load the wikidata:

```
./wb-java.sh org.wikibrain.dao.load.PipelineLoader -f -s wikidata:on
```

You can then get statements about a particular page:

```java
Env env = new EnvBuilder().build();
Configurator conf = env.getConfigurator();
LocalPageDao lpDao = conf.get(LocalPageDao.class);
WikidataDao wdDao = conf.get(WikidataDao.class);
Language simple = Language.getByLangCode("simple");

// Minneapolis's page and its factual statements
Title title = new Title("Minneapolis", simple);
LocalPage mpls = lpDao.getByTitle(title, NameSpace.ARTICLE);
Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(mpls);

// Display the properties
System.out.println("Properties for " + title); 
for (String property : statements.keySet()) {
    List<String> values = new ArrayList<String>();
    for (LocalWikidataStatement lws : statements.get(property)) {
        values.add(lws.getValue());
    }       
    System.out.println("\t" + property + ": " + StringUtils.join(values, ","));
} 
```

As of January 2014, this program outputs 43 statements related to 22 properties of Minneapolis:

```
Properties for Minneapolis:
        located next to body of water -->  Mississippi
        topic's main category -->  Category:Minneapolis
        is in the administrative unit -->  Hennepin County,Minnesota
        flag image -->  Flag of Minneapolis, Minnesota.svg
        instance of -->  county seat,city
        coat of arms image -->  Minneapolis seal.gif
        coordinate location -->  {precision=0, longitude=-93, latitude=44, globe=http://www.wikidata.org/entity/Q2}
        legislative body -->  Minneapolis City Council
        official website -->  http://www.minneapolismn.gov/
        shares border with -->  Lauderdale,Roseville,St. Anthony,Columbia Heights,Fridley,Brooklyn Center,Robbinsdale,Golden Valley,St. Louis Park,Edina,Richfield,Fort Snelling,Saint Paul
        twin city -->  Santiago,Kuopio,Ibaraki,Novosibirsk,Tours,Harbin,Eldoret,Cuernavaca
        country -->  United States of America
        Freebase identifier -->  /m/0fpzwf
        type of administrative division -->  city of the United States
        flag -->  Flag of Minneapolis
        head of government -->  R. T. Rybak
        MusicBrainz area ID -->  3e80aaa7-9b71-450f-8147-0ecf101d8f1a
        dmoz -->  Regional/North_America/United_States/Minnesota/Localities/M/Minneapolis/
        seal description -->  Seal of Minneapolis
        seal image -->  Minneapolis seal.gif
        Commons category -->  Minneapolis, Minnesota
        Wikivoyage banner -->  Minneapolis Stone Arch Bridge banner.jpg
```

Note that these relationships are *structured*, not just textual. For example, the string "R. T. Rybak" is linked to the multilingual concept "R. T. Rybak," and the lat/long coordinates are accessible as a geographic data structure.
