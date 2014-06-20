---
    layout: default
    title: Installation
---

# System requirements and installation 

### Hardware Requirements

WikiBrain's hardware requirements vary depending on the languages you import and modules you use.
As an example, below are approximate disk and memory requirements for different language editions of Wikipedia.
Runtimes are for a fairly powerful desktop circa 2014 (e.g. Intel i7, 8 cores). 

| language         | # pages | # links | memory | disk  | runtime  |
|------------------|---------|---------|--------|-------|----------|
| Simple English   | 100K    | 3M      | 2GB    | 4GB   | 8 min    |
| German           | 1.9M    | 96M     | 8GB    | 80GB  | 300 min  |
| Full English     | 4.6M    | 470M    | 32GB   | 250GB | 1000 min |
| Largest 25 langs | 25M     | 1.6B    | 32GB   | 500GB | 3000 min |


The runtimes above are for the **core** data import (pages, links, categories, etc), and do not include the time
to download the data files or import non-core data.   In addition:

 * Wikidata takes an additional 30 - 120 minutes depending on the language edition installed.
 * Spatial data takes 20 minutes to a few hours depending on the language editions installed.
 * Building the semantic relatedness algorithms can take a few minutes to a few days depending on the algorithms and language editions. 

### Software Requirements
* Maven, Bash, a clone of this repository (instructions in next section).
* Hardware varies depending on the languages you want to import:
  * Simple English (175K articles) requires a few GB and 10 minutes of processing on a four core laptop.
  * Full English (4M articles) requires 200GB and 6 hours of processing on an eight core server.



### Installing WikiBrain

1. If necessary, download and install Sun's JDK 6 or higher.
2. If necessary, download and install [Maven](http://maven.apache.org/download.cgi). tl;dr: 1) unzip the maven download, 2) set the `M2_HOME` environment variable to point to the unzipped directory, 3) make sure the mvn script in `$M2_HOME` is on your `PATH`. You can test your install by making sure that `mvn --version` works properly
3. Clone this repository and run the unit tests to make sure your environment is setup properly:

{% highlight sh %}
cd wikibrain
git clone https://github.com/shilad/wikibrain.git
mvn test
{% endhighlight %}


### Running WikiBrain programs

**From an IDE:** If you are using an IDE such as Eclipse or IntelliJ, and your project is integrated with maven you can run these commands directly through your IDE.

**From the command line:** Install our `wb-java.sh` helper bash script that makes it easier to compile and run java programs. 

```
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
```

**JVM options:** Set reasonable java options defaults. For example `-d64 -Xmx8000M -server` uses a 64-bit JVM with 8GB memory and server optimizations. You can set these defaults in your IDE's run dialog, or if you are using `wb-java.sh`, run the command: `export JAVA_OPTS="-d64 -Xmx8000M -server"`

### Importing data

Download and process the dataset:

```bash
wb-java.sh org.wikibrain.dao.load.PipelineLoader -l simple
```


The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. It imports the data into an embedded h2 database. You can customize WikiBrain's importing procedure, (see Configuration, below) but the default should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

```bash
wb-java.sh org.wikibrain.dao.load.PipelineLoader -l en,fr
``` 
(beware that this is a lot of data and takes many hours!).


### An example program
Once you have imported data (above), your are ready to write programs that analyze Wikipedia!
Here's a [simple example](https://github.com/shilad/wikibrain/blob/master/wikibrain-cookbook/src/main/java/org/wikibrain/phrases/cookbook/ResolveExample.java) you can find in the Cookbook:

{% highlight java %}

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

{% endhighlight %}

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
First, we create an ```Env``, a WikiBrain environment that provides access to the components we need:
```java
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();
```
The [```EnvBuilder```](wikibrain-core/src/main/java/org/wikibrain/core/cmd/EnvBuilder.java) 
provides utility methods to set the languages you want to support, the maximum number of threads available to your program, etc.
There are more advanced ways of configuring WikiBrain - both programatically and through configuration files - described in the Configuration section of this page.

The Env provides access to a 
[```Configurator```](wikibrain-utils/src/main/java/org/wikibrain/conf/Configurator.java) -
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


### Semantic relatedness algorithms
WikiBrain provides several state-of-the-art semantic relatedness algorithms (*SR metrics*). These algorithms estimate the strength of semantic relationships between concepts. 
These algorithms are designed to be fast, with performance of 10-100 milliseconds and caching and multi-threaded support built in.
WikiBrain SR metrics support six major functions:

* `similarity(phrase1, phrase2)` returns the relatedness score between two phrases.
* `similarity(page1, page2)` returns the relatedness score between two pages.
* `mostSimilar(phrase)` returns the most similar phrases to a particular target phrase.
* `mostSimilar(page)` returns the most similar pages to a particular target page.
* `cosimilarity(rowPhrases[], colPhrases[])` computes a cosimilarity matrix for the specified row and column phrases.
* `cosimilarity(rowPages[], colPages[])` computes a cosimilarity matrix for the specified rows and column pages.

To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *inlink* metric):

```bash
./wb-java.sh org.wikibrain.sr.SRBuilder -m inlink
```

The inlink metric is a fast but relatively inaccurate SR metric. You can also build the "ensemble" metric that provides a linear combination of four other metrics. Beware that training the ensemble is costly. It takes about 10 minutes on Simple English Wikipedia, and a little over a day on the full Wikipedia. Most of the model-building time supports the *mostSimilar()* call, so you can speed up model building if you only need *similarity()*. TODO: explain how to do this.

After you build the model for an SR metric, you can use it in your Java application. For example, to use the `mostSimilar()` method for phrases, do the following:

```java    
// Initialize the WikiBrain environment and get the local page dao
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
### Wikidata
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

### Spatial
To intialize the spatial data, you should have [PostGIS](http://postgis.net/install) installed. The library was tested on Postgres 9.3.4 with PostGIS 2.12 on Mac OS X. After installing PostGIS, create a new database, connect to the new database and run the following SQL to enable spatial support:

```text
-- Enable PostGIS (includes raster)
CREATE EXTENSION postgis;
```
Then, go to the [reference.conf](wikibrain-core/src/main/resources/reference.conf) and configure the following settings corresponding to your PostGIS settings.

```text
spatial : {

    dao : {

        dataSource : {

                // These all use keys standard to Geotools JDBC
                // see: http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html

                #change this part according to your DB settings
                default : postgis
                postgis : {
                    dbtype : postgis
                    host : localhost
                    port : 5432
                    schema : public
                    database : wikibrain
                    user : toby
                    passwd : ""
                    max connections : 19
                }
            }
```

Loading the Wikidata layer in the spatial module also requires having Wikidata loaded (see the Wikidata section of this README file)

Now you can load the Wikidata layer by running:

```bash
./wb-java.sh org.wikibrain.spatial.loader.SpatialDataLoader
```

Try running [CalculateGeographicDistanceBetweenPages](/wikibrain-spatial/src/main/java/org/wikibrain/spatial/cookbook/CalculateGeographicDistanceBetweenPages.java). If it runs correctly, the spatial module is successfully initialized.

### Advanced Configuration
The behavior of WikiBrain can be customized through configuration files or code.
The default WikiBrain configuration is determined by the default [reference.conf](wikibrain-core/src/main/resources/reference.conf).
The configuration is backed by [Typesafe config](https://github.com/typesafehub/config) and uses the [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md).
To override the configuration settings create your own configuration file containing the changes to reference.conf and pass it to the EnvBuilder.

For example, suppose we wanted to set the root directory, maxThreads to 8, and the phrase analyzer to the anchortext-based analyzer
We could create a file called myapp.conf containing:

```text
maxThreads : 8
baseDir : /my/path/to/wikibrain
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
        .setBaseDir("/my/path/to/wikibrain")
        .setProperty("phrases.analyzer.default", "anchortext")
        .build();
```

### Using external databases
By default, wikibrain uses an embedded [h2 database](http://www.h2database.com/html/main.html). While this is convenient, it does not scale well. For language editions with more than 1M articles, Postgres is recommended.

You can configure the project to use postgresql by adjusting the configuration as stated above. The relevant section of the [default reference.conf](wikibrain-core/src/main/resources/reference.conf) is:

```
dao : {
    dataSource : {
        default : h2
        h2 : {
           driver : org.h2.Driver
           url: "jdbc:h2:"${baseDir}"/db/h2"
           username : sa
           password : ""
        }
        psql : {
           driver : org.h2.Driver
           url: "jdbc:postgresql://localhost/wikibrain"
           username : grails
           password : ""
        }
    }
}
```

You could override these by creating an external override configuration file (i.e. `override.conf`) with:

```
	dao.dataSource.default : psql
	dao.dataSource.psql.username : foo
	dao.dataSource.psql.password : bar
	dao.dataSource.psql.url : "jdbc:postgresql://localhost/my_database_name"
```

You could then load the altered configuration using:

```
	Env env = new EnvBuilder()
	        .setConfigFile("./path/to/override.conf")
	        .build();
```
Alternately, you could pass the configuration settings directly to the builder:

```

	Env env = new EnvBuilder()
	        .setProperty("dao.dataSource.default", "psql")
	        .setProperty("dao.dataSource.psql.username", "foo")
	        .setProperty("dao.dataSource.psql.password", "bar")
	        .setProperty("dao.dataSource.psql.url", "jdbc:postgresql://localhost/my_database_name")
	        .build();
```
 