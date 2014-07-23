---
    layout: default
    title: Installation
---

# System requirements and installation 

## Hardware Requirements

WikiBrain's hardware requirements vary depending on the languages you import and modules you use.
As an example, below are approximate disk and memory requirements for different language editions of Wikipedia.
. 

| language         | # pages | # links | memory | disk  | runtime  |
|------------------|---------|---------|--------|-------|----------|
| Simple English   | 100K    | 3M      | 2GB    | 4GB   | 8 min    |
| German           | 1.9M    | 96M     | 8GB    | 80GB  | 300 min  |
| Full English     | 4.6M    | 470M    | 8GB   | 250GB | 1000 min |
| Largest 25 langs | 25M     | 1.6B    | 8GB   | 500GB | 3000 min |

<br/>
The runtimes above used a fairly powerful desktop circa 2014 (e.g. Intel i7, 8 cores).
They only include importing the **core** data import (pages, links, categories, etc) and do not include the time to download the data files.  

Other modules take additional time and resources:

 * Wikidata takes an additional 30 - 120 minutes depending on the language edition installed.
 * Spatial data takes 20 minutes to a few hours depending on the language editions installed.
 * Building the semantic relatedness algorithms can take a few minutes to a few days depending on the algorithms and language editions. 

## Software Requirements

Wikibrain requires a Java JDK, version 6 or higher. 

Depending on your needs you may also consider installing these **optional software** packages:

1. **Java IDE**. You can directly run Wikibrain programs through any standard Java IDE (Eclipse, Netbeans, IntelliJ, etc.).
3. **Bash**. If you would like to run Wikibrain programs from the command line, you'll need the bash shell.
2. **Maven**. The [Maven](http://maven.apache.org/) Java dependency management system, version 3.2 or higher. 
Maven manages Java dependencies and allows you to stay up to date with Wikibrain jars and their dependencies.
The Maven website details installation instructions.
tl;dr: 1) unzip the maven download, 2) set the M2_HOME environment variable to point to the unzipped directory, 3) make sure the mvn script in $M2_HOME is on your PATH. You can test your install by making sure that mvn --version works properly.
4. **Postgresql / PostGIS2**. By default, Wikibrain uses the embedded [h2 database](http://www.h2database.com/). 
While this works well for most language editions, the [Postgresql](http://www.postgresql.org/) database performs better for the largest language editions (such as full English).
If you'd like to use the **spatial module**, you must install Postgres and the PostGIS 2 (or higher) extension. Details on installing Postgresql appear at the bottom of this page.    


## Installing WikiBrain

You can either install WikiBrain by downloading the zip file containing the WikiBrain jars and dependencies or referencing the WikiBrain maven artifacts.
 While maven requires a bit of initial setup, we strongly recommend using it.
  
#### Installing using jars
 
1. Download the [latest WikiBrain zip file](http://shilad.com/wikibrain/wikibrain-and-deps-{{ site.wbversion }}.zip) containing the WikiBrain jars and their dependencies.
2. Extract the zip file.
3. In your IDE, add all jars in the zip file to your classpath.
  
#### Installing using maven

1. Download and install Maven, as described above.
2. Add the following dependency to your project's pom.xml:
{% include maven.html %}

We've posted a complete [example pom.xml](https://gist.github.com/shilad/958ec6f2cab01b34efe9) that references WikiBrain.

## Setting your JVM options

**JVM options:** Set reasonable java options defaults. For example `-d64 -Xmx8000M -server` uses a 64-bit JVM with 8GB memory and server optimizations. 
You can set these defaults in your IDE's run dialog, or if you are using `wb-java.sh` (below), run the command: `export JAVA_OPTS="-d64 -Xmx8000M -server"`
The memory requirements for the JVM during the import phase are listed above.

## Installing the command line wb-java.sh script

If you'd like to run WikiBrain programs from the command line, we've provided a helpful [wb-java.sh](https://github.com/shilad/wikibrain/blob/master/wikibrain-utils/src/main/resources/wb-java.sh) script.
It sets appropriate paths and configures command line arguments.
To use it, first install it:

```bash
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
```

You can now run WikiBrain programs from the command line as follows:

```bash
./wb-java.sh org.wikibrain.dao.loader.PipelineLoader -l simple
```

## Installing Postgresql and PostGIS
 
Notes, TODO: write them up:

 * Postgresql 8.4+
 * For spatial: Postgresql 9.2+, PostGIS 2+
 * Postgresql.app for Mac
 * Standard installer for Windows
 * Linux use official Postgresql repos (e.g. http://wiki.postgresql.org/wiki/Apt)
 * Linux: add the following two lines to /etc/sysctl.conf and reboot:
```
kernel.shmmax = 64205988352
kernel.shmall = 15675290
```
 * Read the performance tuning page: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
 * Crucial settings:

```
    listen_addresses = '*'
    max_connections = 500         # Must be at least 300
    shared_buffers = 48GB         # Should be 1/4 of system memory
    effective_cache_size = 96GB   # Should be 1/2 of system memory
    fsync = off                 
    synchronous_commit = off    
    checkpoint_segments = 256
    checkpoint_completion_target = 0.9


```
