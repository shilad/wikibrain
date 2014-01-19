wikAPIdia
=====
The WikAPIdia Java framework provides easy and efficient access to multi-lingual Wikipedia data.

###Main features
* Support for **all Wikipedia language** editions and comparisons between multiple languages.
* Tools that **download** and organize [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.
* Tools that load downloaded Wikipedia datasets into **databases** for analysis.
* Tools that identify **multi-lingual concepts** and the pages in each language edition that link to those concepts.
* **Semantic-relatedness algorithms** that measure the relationship between two concepts such as "racecar" and "engine."
* Single-machine **parallelization** (i.e. multi-threading support) for all computationally intensive features.

###System Requirements
* Maven, Bash, a clone of this repository (instructions in next section).
* Hardware varies depending on the languages you want to import:
  * Simple English (175K articles) requires a few GB and 10 minutes of processing on a four core laptop.
  * Full English (4M articles) requires 200GB and 6 hours of processing on an eight core server.

###Installing WikAPIdia

1. If necessary, download and install Sun's JDK 6 or higher.
2. If necessary, download and install [Maven](http://maven.apache.org/download.cgi). tl;dr: 1) unzip the maven download, 2) set the `M2_HOME` environment variable to point to the unzipped directory, 3) make sure the mvn script in `$M2_HOME` is on your `PATH`. You can test your install by making sure that `mvn --version` works properly
3. Clone this repository and run the unit tests to make sure your environment is setup properly:

```bash
cd wikAPIdia
git-clone https://github.com/shilad/wikAPIdia.git
mvn -f wikAPIdia-parent/pom.xml test
```

###Running WikAPIdia programs

**From an IDE:** If you are using an IDE such as Eclipse or IntelliJ, and your project is integrated with maven you can run these commands directly through your IDE.

**From the command line:** Install our `wp-java.sh` helper bash script that makes it easier to compile and run java programs. `mvn -f wikAPIdia-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikapidia.utils.ResourceInstaller`

**JVM options:** Set reasonable java options defaults. For example `-d64 -Xmx8000M -server` uses a 64-bit JVM with 8GB memory and server optimizations. You can set these defaults in your IDE's run dialog, or if you are using `wp-java.sh`, run the command: `export JAVA_OPTS="-d64 -Xmx8000M -server"`

###Importing data

Download and process the dataset:

```bash
wp-java.sh org.wikapidia.dao.load.PipelineLoader -l simple
```


The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. It imports the data into an embedded h2 database. You can customize WikAPIdia's importing procedure, (see Configuration, below) but the default should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

```bash
wp-java.sh org.wikapidia.dao.load.PipelineLoader -l en,fr
``` 
(beware that this is a lot of data and takes many hours!).


###An example program
Once you have imported data (above), your are ready to write programs that analyze Wikipedia!
Here's a [simple example](https://github.com/shilad/wikAPIdia/blob/master/wikAPIdia-cookbook/src/main/java/org/wikapidia/phrases/cookbook/ResolveExample.java) you can find in the Cookbook:

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

###A tour of the example
Let's walk through this program to explain each piece. 
First, we create an ```Env``, a WikAPIdia environment that provides access to the components we need:
```java
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();
```
The [```EnvBuilder```](wikAPIdia-core/src/main/java/org/wikapidia/core/cmd/EnvBuilder.java) 
provides utility methods to set the languages you want to support, the maximum number of threads available to your program, etc.
There are more advanced ways of configuring WikAPIdia - both programatically and through configuration files - described in the Configuration section of this page.

The Env provides access to a 
[```Configurator```](wikAPIdia-utils/src/main/java/org/wikapidia/conf/Configurator.java) -
essentially a Factory for creating WikAPIdia components. We get the Page Resolution component next:
```java
Configurator configurator = env.getConfigurator();
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class);
```

A key feature of WikAPIdia is that it supports multiple implementations of the same component. 
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

###Main components
The WikAPIdia Configurator offers a set of components that you can use as building blocks in your application.
To get one of these components, use the Configurator.get() method:
* **RawPageDao** provides detailed information about an article, include the raw WIkiMarkup pagetext.
* **LocalPageDao** provides basic metadata about an article, including title, namespace, and Wikipedia id.
* **LocalLinkDao** provides access to the Wikilink structure between pages.
* **LocalCategoryMemberDao** provides access to Wikipedia's category graph.
* **UniversalArticleDao** provides access to the multilingual concept mapping.
* **UniversalLinkDao** exposes the link structure imposed by the multilingual mapping.
* **LuceneSearcher** searches arbitrary fields (e.g. title or plain text) in an arbitrary language.
* **SparseMatrix** represents a sparse matrix of ints (ids) to floats (values) that is persisted using memory mapping to disk.
* **PhraseAnalyzer** returns the most likely Wikipedia articles for a textual phrase, and the most common textual phrases that represent a particular Wikipedia article.
* **MonolingualSRMetric** returns the strength of relationship between two Wikipedia pages or phrases in a particular language, and the most closely related pages to a particular phrase or page.
* **UniversalSRMetric** (not yet tested) returns the same information as the MonolingualSRMetric, but for universal concepts that span multiple languages.


###Semantic relatedness algorithms
WikAPIdia provides several state-of-the-art semantic relatedness algorithms (*SR metrics*). These algorithms estimate the strength of semantic relationships between concepts. 
These algorithms are designed to be fast, with performance of 10-100 milliseconds and caching and multi-threaded support built in.
WikAPIdia SR metrics support six major functions:

* `similarity(phrase1, phrase2)` returns the relatedness score between two phrases.
* `similarity(page1, page2)` returns the relatedness score between two pages.
* `mostSimilar(phrase)` returns the most similar phrases to a particular target phrase.
* `mostSimilar(page)` returns the most similar pages to a particular target page.
* `cosimilarity(rowPhrases[], colPhrases[])` computes a cosimilarity matrix for the specified row and column phrases.
* `cosimilarity(rowPages[], colPages[])` computes a cosimilarity matrix for the specified rows and column pages.

To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *inlink* metric):

```bash
./wp-java.sh org.wikapidia.sr.SRBuilder -m inlink
```

The inlink metric is a fast but relatively inaccurate SR metric. You can also build the "ensemble" metric that provides a linear combination of four other metrics. Beware that training the ensemble is costly. It takes about 10 minutes on Simple English Wikipedia, and a little over a day on the full Wikipedia. Most of the model-building time supports the *mostSimilar()* call, so you can speed up model building if you only need *similarity()*. TODO: explain how to do this.

After you build the model for an SR metric, you can use it in your Java application. For example, to use the `mostSimilar()` method for phrases, do the following:

```java    
// Initialize the WikAPIdia environment and get the local page dao
Env env = new EnvBuilder().build();
Configurator conf = env.getConfigurator();
LocalPageDao lpDao = conf.get(LocalPageDao.class);
Language simple = Language.getByLangCode("simple");

// Retrieve the "ensemble" sr metric for simple english 
MonolingualSRMetric sr = conf.get(
        MonolingualSRMetric.class, "ensemble",
        "language", simple.getLangCode());

//Similarity between strings 
for (String phrase : Arrays.asList("Barack Obama", "US", "Canada", "vim")) {
    SRResultList similar = sr.mostSimilar(phrase, 3);
    List<String> pages = new ArrayList<String>();
    for (int i = 0; i < similar.numDocs(); i++) {
        LocalPage page = lpDao.getById(simple, similar.getId(i));
        pages.add((i+1) + ") " + page.getTitle());
    }       
    System.out.println("'" + phrase + "' is similar to " + StringUtils.join(pages, ", ")); 
}  
```
This code (on Simple english) displays:
```
*Barack Obama* is similar to 1) Barack Obama, 2) Hillary Rodham Clinton, 3) Mitt Romney
*US* is similar to 1) United States, 2) Federal government of the United States, 3) United States Constitution
*Canada* is similar to 1) Canada, 2) Quebec, 3) Australia
*vim* is similar to 1) Vim, 2) Text editor, 3) GNU Emacs
```
You can also calculate a relatedness score between any two pages or phrases using the same MonolingualSRMetric:
```java
//Similarity between strings 
String pairs[][] = new String[][] {
        { "cat", "kitty" },
        { "obama", "president" },
        { "tires", "car" },
        { "java", "computer" },
        { "dog", "computer" },
};      

for (String pair[] : pairs) {
    SRResult s = sr.similarity(pair[0], pair[1], false); 
    System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'"); 
}    
```
This code (on Simple english) displays:
```
0.7652857508808703: 'cat', 'kitty'
0.6816860201660842: 'obama', 'president'
0.7058954876236786: 'tires', 'car'
0.5905978329192705: 'java', 'computer'
0.42989849626985877: 'dog', 'computer'
```
###Wikidata
Wikipedia's Wikidata initiative "aims to create a free knowledge base about the world that can be read and edited by humans and machines alike." In short, the initiative shifts the "facts" that support Wikipedia articles (Minneapolis is in Minnesota) into a structured universal repository that can be accessed in any language.

The WikAPIdia library includes support for Wikidata. To use it, you must first import the Wikidata archive. This is a relatively large dataset (~10GB uncompressed), so it is not loaded by default during the regular pipeline. 

To explicitly load wikidata, AFTER you've loaded the default data, run:

```
./wp-java.sh org.wikapidia.dao.load.PipelineLoader -f -s wikidata:on
```

You can then get statements about a particular page:

```java
Env env = new EnvBuilder().build();
Configurator conf = env.getConfigurator();
LocalPageDao lpDao = conf.get(LocalPageDao.class);
WikidataDao wdDao = conf.get(WikidataDao.class);
Language simple = Language.getByLangCode("simple");

// Get Barack Obama's page and its factual statements
Title title = new Title("Barack Obama", simple);
LocalPage obama = lpDao.getByTitle(title, NameSpace.ARTICLE);
Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(obama);

// Display the properties
System.out.println("Properties for " + title); 
for (String property : statements.keySet()) {
    System.out.println("values for property " + property + " are:"); 
    for (LocalWikidataStatement lws : statements.get(property)) {
        System.out.println("\t" + lws.getFullStatement());
    }       
}   
```

This program would output:

```
```


###Advanced Configuration
The behavior of WikAPIdia can be customized through configuration files or code.
The default WikAPIdia configuration is determined by the main [reference.conf](wikAPIdia-core/src/main/resources/reference.conf).
The configuration is backed by [Typesafe config](https://github.com/typesafehub/config) and uses the [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md).
To override the configuration settings create your own configuration file containing the changes to reference.conf and pass it to the EnvBuilder.

For example, suppose we wantd to set the root directory, maxThreads to 8, and the phrase analyzer to the anchortext-based analyzer
We could create a file called myapp.conf containing:
```text
maxThreads : 8
baseDir : /my/path/to/wikAPIdia
phrases.analyzer.default : anchortext
```
We would then tell the EnvBuilder to use the new config to override the default settings:
```java
Env env = new EnvBuilder()
        .setConfigFile("./path/to/myapp.conf")
        .build();
```
We could also make these changes directly in Java, without the config file:
```java
Env env = new EnvBuilder()
        .setMaxThreads(8)
        .setBaseDir("/my/path/to/wikAPIdia")
        .setProperty("phrases.analyzer.default", "anchortext")
        .build();
```
