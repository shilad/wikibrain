---
    layout: default
    title: Installation
---

# System requirements and installation 

### Hardware Requirements

WikiBrain's hardware requirements vary depending on the languages you import and modules you use.
As an example, below are approximate disk and memory requirements for different language editions of Wikipedia.
. 

| language         | # pages | # links | memory | disk  | runtime  |
|------------------|---------|---------|--------|-------|----------|
| Simple English   | 100K    | 3M      | 2GB    | 4GB   | 8 min    |
| German           | 1.9M    | 96M     | 8GB    | 80GB  | 300 min  |
| Full English     | 4.6M    | 470M    | 32GB   | 250GB | 1000 min |
| Largest 25 langs | 25M     | 1.6B    | 32GB   | 500GB | 3000 min |

<br/>
The runtimes above used a fairly powerful desktop circa 2014 (e.g. Intel i7, 8 cores).
They only include importing the **core** data import (pages, links, categories, etc) and do not include the time to download the data files.  

Other modules take additional time and resources:

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
