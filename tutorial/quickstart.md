---
    layout: default
    title: Quickstart
---
        
# Quickstart 

As a quick start, we'll load Wikipedia data and foo.

## System requirements for this example:

In order to complete this quickstart, you need a basic Java development environment. In particular: 

* Sun's Java JDK 6 or higher.
* Some Java IDE (Eclipse, IntelliJ, NetBeans, etc).
* At least 4GB of memory.
* At least 4GB of free hard disk space.

These three requirements will enable you to complete this tutorial, however your hardware and software requirements will vary depending on your software application and goals.
For example, you can install WikiBrain using command line shell scripts, you can include it using maven dependency management system, and
you'll need substantially more powerful hardware to process full (not simple) English Wikipedia.

More details on installation are described in the [system requirements]({{ site.url }}/tutorial/requirements.html) section of the tutorial.

## Install Wikibrain

* Download and extract the [WikiBrain plus dependencies archive].
* Double click the extracted jar file. You should see the GUI import screen (below).

## Import Simple English

Next, you'll import Simple English Wikipedia.
Run the pipeline loader which downloads, parses, and imports the data into an embedded [h2 database](http://www.h2database.com/html/main.html) (you can use Postgresql instead by changing the [configuration file]({{ site.url }}/tutorial/configuration.html)).
The details for running the loader are as follows:
 
 * main class: `org.wikibrain.Loader`
 * JVM args: `-Xmx4G`
 * program args: `-l simple`

In IntelliJ, this looks like the following:

![PipelineLoader]({{ site.baseurl }}/images/run-pipeline.png)

The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. 
It imports the data into an embedded h2 database. 
You can customize WikiBrain's importing procedure, (see Configuration) but the default should be a good start. 

## An example program

* Download and extract the zipfile containing the jars (TODO: link zipfile).
* Add the jar files to the classpath of your project. 

Once you have your imported data (above), your are ready to write programs that analyze Wikipedia!

Begin by creating the Java project in your IDE of choice (NetBeans, Eclipse, IntelliJ, etc). Add the WikiBrain plus dependencies jar file (that you downloaded in the previous step) to your java project.

Start writing your program! Here's a [simple example](https://github.com/shilad/wikibrain/blob/master/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/Quickstart.java) you can find in the Cookbook:

<script src="http://gist-it.appspot.com/github/shilad/wikibrain/blob/master/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/Quickstart.java?slice=13:40"></script>


When you run this program, you'll see output:

```text
resolution of apple
	LocalPage{nameSpace=ARTICLE, title=Apple, localId=39, language=Simple English}: 0.070175424
	LocalPage{nameSpace=ARTICLE, title=Apple juice, localId=19351, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Macintosh, localId=517, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Inc., localId=7111, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple A4, localId=251288, language=Simple English}: 0.043859642
```

## A tour of the example
Let's walk through this program to explain each piece. 
First, we create an ```Env``, a WikiBrain environment that provides access to the components we need:

```java
Env env = EnvBuilder.envFromArgs(args);
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
