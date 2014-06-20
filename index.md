---
        layout: default
        title: Homepage
---
WikiBrain
=====
WikiBrain is a Java library that democratizes access to a range of Wikipedia-based algorithms and technologies. 
WikiBrain allows researchers and developers to easily include state-of-the-art algorithms that mine Wikipedia in their applications with a few lines of code.


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

### A tour of the example
Let's walk through this program to explain each piece. 
First, we create an `Env`, a WikiBrain environment that provides access to the components we need:

```java
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();
```

The [```EnvBuilder```](wikibrain-core/src/main/java/org/wikibrain/core/cmd/EnvBuilder.java) 
provides utility methods to set the languages you want to support, the maximum number of threads available to your program, etc.
There are more advanced ways of configuring WikiBrain - both programatically and through configuration files - described in the Configuration section of this page.

The Env provides access to a 
[`Configurator`](wikibrain-utils/src/main/java/org/wikibrain/conf/Configurator.java) -
essentially a Factory for creating WikiBrain components. We get the Page Resolution component next:

```java
Configurator configurator = env.getConfigurator();
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class);
```

A key feature of WikiBrain is that it supports multiple implementations of the same component. 
For example, the default PhraseAnalayzer uses the [Lucene](http://lucene.apache.org/) search engine. 
We could have explicitly requested the lucene implementation of the PhraseAnalyzer:

```java
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "lucene");
```
If we instead wanted to use a phrase analyzer that resolves phrases to pages by looking at "intra-wiki" links, we could have used:

```java
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "anchortext");
```

And received the results:

```text
resolution of apple
	LocalPage{nameSpace=ARTICLE, title=Apple, localId=39, language=Simple English}: 0.55263156
	LocalPage{nameSpace=ARTICLE, title=Apple Inc., localId=7111, language=Simple English}: 0.30526316
	LocalPage{nameSpace=ARTICLE, title=Apple Records, localId=47698, language=Simple English}: 0.12631579
	LocalPage{nameSpace=ARTICLE, title=App Store (iOS), localId=216566, language=Simple English}: 0.010526316
	LocalPage{nameSpace=ARTICLE, title=Apple Corps, localId=48013, language=Simple English}: 0.005263158
```

### Main components
The WikiBrain Configurator offers a set of components that you can use as building blocks in your application.
To get one of these components, use the Configurator.get() method:

* **RawPageDao** provides detailed information about an article, include the raw WIkiMarkup pagetext.
* **LocalPageDao** provides basic metadata about an article, including title, namespace, and Wikipedia id.
* **LocalLinkDao** provides access to the Wikilink structure between pages.
* **LocalCategoryMemberDao** provides access to Wikipedia's category graph.
* **UniversalArticleDao** provides access to the multilingual concept mapping.
* **UniversalLinkDao** exposes the link structure imposed by the multilingual mapping.
* **WikidataDao** stores factual statements, aliases, and descriptions about to multilingual concepts.
* **LuceneSearcher** searches arbitrary fields (e.g. title or plain text) in an arbitrary language.
* **SparseMatrix** represents a sparse matrix of ints (ids) to floats (values) that is persisted using memory mapping to disk.
* **PhraseAnalyzer** returns the most likely Wikipedia articles for a textual phrase, and the most common textual phrases that represent a particular Wikipedia article.
* **MonolingualSRMetric** returns the strength of relationship between two Wikipedia pages or phrases in a particular language, and the most closely related pages to a particular phrase or page.
* **UniversalSRMetric** (not yet tested) returns the same information as the MonolingualSRMetric, but for universal concepts that span multiple languages.
