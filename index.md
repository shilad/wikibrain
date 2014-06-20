---
        layout: default
        title: Homepage
---

The WikiBrain Java library democratizes access to state-of-the-art Wikipedia-based algorithms and technologies. 
WikiBrain allows researchers and developers to incorporate algorithms that mine Wikipedia in their applications with a few lines of code.


WikiBrain's busy thinking up its first public release. If you have questions, ask the [WikiBrain google group](https://groups.google.com/forum/#!forum/wikibrain).

### Main features
* Support for **all Wikipedia language** editions and comparisons between multiple languages.
* Tools that **download** and organize [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.
* Tools that load downloaded Wikipedia datasets into **databases** for analysis.
* Tools that identify **multi-lingual concepts** and the pages in each language edition that link to those concepts.
* **Semantic-relatedness algorithms** that measure the relationship between two concepts such as "racecar" and "engine."
* Support for structured [Wikidata](http://meta.wikimedia.org/wiki/Wikidata) "facts" about articles.
* Single-machine **parallelization** (i.e. multi-threading support) for all computationally intensive features.

### An example program
Once you have imported data (above), your are ready to write programs that analyze Wikipedia!
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


### Main components
The WikiBrain Configurator offers a set of components that you can use as building blocks in your application.
To get one of these components, use the Configurator.get() method:

* *RawPageDao* provides detailed information about an article, include the raw WIkiMarkup pagetext.
* *LocalPageDao* provides basic metadata about an article, including title, namespace, and Wikipedia id.
* *LocalLinkDao* provides access to the Wikilink structure between pages.
* **LocalCategoryMemberDao** provides access to Wikipedia's category graph.
* **UniversalArticleDao** provides access to the multilingual concept mapping.
* **UniversalLinkDao** exposes the link structure imposed by the multilingual mapping.
* **WikidataDao** stores factual statements, aliases, and descriptions about to multilingual concepts.
* **LuceneSearcher** searches arbitrary fields (e.g. title or plain text) in an arbitrary language.
* **SparseMatrix** represents a sparse matrix of ints (ids) to floats (values) that is persisted using memory mapping to disk.
* **PhraseAnalyzer** returns the most likely Wikipedia articles for a textual phrase, and the most common textual phrases that represent a particular Wikipedia article.
* **MonolingualSRMetric** returns the strength of relationship between two Wikipedia pages or phrases in a particular language, and the most closely related pages to a particular phrase or page.
* **UniversalSRMetric** (not yet tested) returns the same information as the MonolingualSRMetric, but for universal concepts that span multiple languages.
