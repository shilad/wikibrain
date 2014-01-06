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

**From an IDE** If you are using an IDE such as Eclipse or IntelliJ, and your project is integrated with maven you can run these commands directly through your IDE.

**From the command line** Install our `wp-java.sh` helper bash script that makes it easier to compile and run java programs. `mvn -f wikAPIdia-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikapidia.utils.ResourceInstaller`.

**JVM options** Set reasonable java options defaults. For example `-d64 -Xmx8000M -server` uses a 64-bit JVM with 8GB memory and server optimizations. You can set these defaults in your IDE's run dialog, or if you are using `wp-java.sh`, run the command: `export JAVA_OPTS="-d64 -Xmx8000M -server"`

###Importing data

Download and process the dataset:

```bash
wp-java.sh org.wikapidia.dao.load.PipelineLoader -l simple
```


The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. 

You can customize WikAPIdia's importing procedure, but the Pipeline should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

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
To get one of these components, use the Configurator.get() method.
TODO: List and one-sentence description of most important components

###Configuration
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
