---
        layout: default
        title: Homepage
---


**WikiBrain's busy thinking up its first public release. Please be patient while we fine tune our APIs and complete our documentation. Ask us questions at the [WikiBrain google group](https://groups.google.com/forum/#!forum/wikibrain)!**

The WikiBrain Java library enables researchers and developers to incorporate state-of-the-art Wikipedia-based algorithms and technologies in a few lines of code.

**WikiBrain is easy to use**.
Wikipedia data can be downloaded, parsed, and imported into a database by running a single command.
WikiBrain allows you to incorporate state-of-the art algorithms in your Java projects in just a few lines of code. 

**WikiBrain is multi-lingual**.
WikiBrain supports all 267 Wikipedia language editions, and builds a concept-map that connects an article in one language to the same article in another langauge.

**WikiBrain is fast**.
WikiBrain uses single-machine **parallelization** (i.e. multi-threading) for all computationally intensive features.
While it imports data into standard SQL databases (h2 or Postgres), it builds optimized local caches for critical data. 

WikiBrain integrates a variety of specific algorithms and datasets in one framework, including: 

* **SR:** Semantic-relatedness algorithms that measure the strength of association between two concepts such as "racecar" and "engine."
* **Spatial:** WikiBrain provides geospatial algorithms for spatial Wikipedia pages like [Minnesota](http://en.wikipedia.org/wiki/Minnesota) and the [Eiffel Tower](http://en.wikipedia.org/wiki/Eiffel_Tower).      
* **Wikidata:** Support for structured [Wikidata](http://meta.wikimedia.org/wiki/Wikidata) "facts" about articles.
* **Pageviews:** Public data about how often Wikipedia pages are viewed with hourly precision.

## An example program
Once you have [imported data](tutorial/import.html), you can write programs that analyze Wikipedia.
Here's a [simple example](https://github.com/shilad/wikibrain/blob/master/wikibrain-cookbook/src/main/java/org/wikibrain/phrases/cookbook/ResolveExample.java) you can find in the Cookbook:

```java
// Prepare the environment; set the root to the current directory (".").
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();

// Get the configurator that creates components and a phraze analyzer from it 
Configurator configurator = env.getConfigurator();
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class);

// get the most common phrases in simple  
Language simple = Language.getByLangCode("simple");   // simple english 
LinkedHashMap<LocalPage, Float> resolution = pa.resolveLocal(simple, "apple", 5);
        
// show the closest pages
System.out.println("resolution of apple");
if (resolution == null) { 
    System.out.println("\tno resolution !");
} else {
    for (LocalPage p : resolution.keySet()) {
        System.out.println("\t" + p + ": " + resolution.get(p));
    }       
} 
```

When you run this program, you'll see output:

```text
resolution of apple
	LocalPage{nameSpace=ARTICLE, title=Apple, localId=39, language=Simple English}: 0.070175424
	LocalPage{nameSpace=ARTICLE, title=Apple juice, localId=19351, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Macintosh, localId=517, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Inc., localId=7111, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple A4, localId=251288, language=Simple English}: 0.043859642
```


## About

WikiBrain development is led by [Shilad Sen](http://shilad.com) at Macalester College and [Brent Hecht](http://brenthecht.com) at the University of Minnesota.
WikiBrain has been generously supported by the National Science Foundation, Macalester College, and the University of Minnesota.
WikiBrain is licensed under the Apache License, version 2.

**Macalester WikiBrain development team, Summer 2013**
![Summer 2013]({{ site.baseurl }}/images/summer2013.jpg)

WikiBrain has been made possible through substantial contributions by many students, including:
 Alan Morales Blanco,
 Margaret Giesel,
 Rebecca Gold, 
 Becca Harper,
 Ben Hillman,
 Sam Horlbeck,
 Aaron Jiang,
 Matthew Lesicko,
 Toby Li,
 Yulun Li,
 Huy Mai,
 Ben Mathers,
 Sam Naden,
 Jesse Russell,
 Laura Sousa Vonessen,
 Zixiao Wang, 
 and Ari Weilland   
 
 
