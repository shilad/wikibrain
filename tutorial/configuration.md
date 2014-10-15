---
    layout: default
    title: Configuration
---
        

# Configuration

This page describes how to customize your configuration of WikiBrain.
               

## Env, configurator and components

Each WikiBrain runtime environment is associated with an instance of the [Env class](https://github.com/shilad/wikibrain/blob/master/wikibrain-core/src/main/java/org/wikibrain/core/cmd/Env.java).
An Env creates, configures, and keeps track of the components of WikiBrain. 
  

Let's say you want to create a [Disambiguator](https://github.com/shilad/wikibrain/blob/master/wikibrain-sr/src/main/java/org/wikibrain/sr/disambig/Disambiguator.java) component
(a disambiguator returns Wikipedia articles that correspond to a particular phrase). 
They are described in more detail in the [SR section](sr.html) of this manual. 

To create a new WikiBrain environment and get a particular disambiguator you would do the following:
 
```java
public static void main(String args[]) {

    // Prepare the environment
    Env env = EnvBuilder.envFromArgs(args);
    
    // Get the configurator that creates components and a phraze analyzer from it
    Configurator configurator = env.getConfigurator();
            
    // Get a specific disambiguator called "topResult"
    Disambiguator dab1 = configurator.get(Disambiguator.class, "topResult");
    
    // Get the default disambiguator (named "similarity" in this case)
    Disambiguator dab2 = configurator.get(Disambiguator.class);
}
```

Let's walk through this program to explain each piece. 
First, we create an `Env`, a WikiBrain environment that provides access to the components we need:

```java
Env env = EnvBuilder.envFromArgs(args);
```

The [```EnvBuilder```](wikibrain-core/src/main/java/org/wikibrain/core/cmd/EnvBuilder.java) 
provides utility methods to set the languages you want to support, the maximum number of threads available to your program, etc.
There are more advanced ways of configuring WikiBrain - both programatically and through configuration files - described in the WikiBrain command line args section of this page.
You can also create an `Env` by hand, but the builder provides many convenience methods for you.

The Env provides access to a 
[```Configurator```](wikibrain-utils/src/main/java/org/wikibrain/conf/Configurator.java) -
essentially a Factory for creating WikiBrain components. We get the Disambiguator next:

```java
Configurator configurator = env.getConfigurator();
Disambiguator dab1 = configurator.get(Disambiguator.class, "topResult");
```

Finally, you typically want the "default" version of a particular component.
In that case, you can omit its name:
                                                                                    
```java
Disambiguator dab2 = configurator.get(Disambiguator.class);
```

In this case, you'll receive the `similarity` disambiguator, which is an instance of [SimilarityDisambiguator](https://github.com/shilad/wikibrain/blob/master/wikibrain-sr/src/main/java/org/wikibrain/sr/disambig/SimilarityDisambiguator.java).

## Overview of configuration file structure

WikiBrain decides how to configure components by looking at its configuration files. 
The default configuration file is stored in [reference.conf](https://github.com/shilad/wikibrain/blob/master/wikibrain-core/src/main/resources/reference.conf).
You should _NOT_ edit the reference.conf file.
Instead, you can specify configuration files that override these default settings (more on this later).

The configuration system is based on [Typesafe's config framework](https://github.com/typesafehub/config) and uses a JSON-like format called [HOCON](https://github.com/typesafehub/config#using-hocon-the-json-superset).

Consider the snippet below from the default reference.conf, which defines four disambiguators named `topResult`, `topResultConsensus`, etc. 
It also tells WikiBrain to use the `similarity` disambiguator by default. 

```text
sr : {
    disambig : {
        default : similarity
        topResult : {
            type : topResult
            phraseAnalyzer : default
        }
        topResultConsensus : {
            type : topResultConsensus
            phraseAnalyzers : ["lucene","stanford","anchortext"]
        }
        milnewitten : {
            type : milnewitten
            metric : milnewitten
            phraseAnalyzer : default
        }
        similarity : {
            type : similarity
            metric : inlinknotrain
            phraseAnalyzer : default

            // how to score candidate senses. Possibilities are:
            //      popularity: just popularity
            //      similarity: just similarity
            //      product: similarity * popularity
            //      sum: similarity + popularity
            criteria : sum
        }
    }
```


## Customizing your configuration
You'll commonly want to override the default reference.conf configuration file.
To do so, create a text file, and include configurations for any elements you'd like.
Any configuration in your file will override reference.conf.

For example, to change the default disambiguator to `topResult`, do the following:

```
sr.disambig.default : topResult
```

To run a program with an override configuration file, you can take advantage of the `-c` option that is processed by `EnvBuilder.envFromArgs()`:

```
$ java my.class.Name -c /path/to/myConf.conf
```

Alternately, we could specify a configuration override in the Java program:

```java
Env env = new EnvBuilder()
        .setConfigFile("/path/to/myConf.conf")
        .build();
```

Or we could change the property directly:
                                         
```java
Env env = new EnvBuilder()
        .setProperty("sr.disambig.default", "topResult")
        .build();
```

## Standard command-line options

WikiBrain specifies a set of standard command-line options.
If your program parses arguments using `EnvBuilder.envFromArgs`, it will recognize them.
Wikibrain recognizes the following arguments:
 
| option     | value              | default             |       notes        |
|------------|--------------------|---------------------|--------------------|
| -c         | path/to/conf.txt   |                     |                    |
| -h         | max-threads        | # logical cores     |                    |
| -l         | language-codes     | installed languages | comma separated    |
| --base-dir | path/to/dir        | current directory   |                    |
| --tmp-dir  | path/to/dir        | baseDir/.tmp        | WikiBrain requires many GB of tmp space |

If you would like to add custom command line options to your program, take a look at [DumpLoader.java](https://github.com/shilad/wikibrain/blob/master/wikibrain-loader/src/main/java/org/wikibrain/dao/load/DumpLoader.java),
  which shows how to incorporate custom command line processing with WikiBrain. 

## Using external databases
By default, wikibrain uses an embedded [h2 database](http://www.h2database.com/html/main.html). 
While this is convenient, it does not scale well and does not currently support WikiBrain's spatial module.
For language editions with more than 1M articles, Postgres is recommended.

You can configure the project to use postgresql by adjusting the configuration as stated above. 
The relevant section of the [default reference.conf](https://github.com/shilad/wikibrain/blob/master/wikibrain-core/src/main/resources/reference.conf) is:

```
dao : {
    dataSource : {
        default : h2
        h2 : {
           driver : org.h2.Driver
           url: "jdbc:h2:"${baseDir}"/db/h2;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;MAX_OPERATION_MEMORY=100000000"
           username : sa
           password : ""

           // Connection pooling
           // This sets the total number of jdbc connections to a minimum of 16.
           // partitions defaults to max(8, num-logical-cores)
           partitions : default
           connectionsPerPartition : 2
        }
        psql : {
           driver : org.postgresql.Driver
           url: "jdbc:postgresql://localhost/wikibrain"
           username : toby
           password : ""

           // Connection pooling
           // This sets the total number of jdbc connections to a minimum of 16.
           // partitions defaults to max(8, num-logical-cores)
           partitions : default
           connectionsPerPartition : 2
        }
    }
```

You could override these by creating an external override configuration file (i.e. `override.conf`) with:

```
	dao.dataSource.default : psql
	dao.dataSource.psql {
	                username : foo
                    password : bar
	                url : "jdbc:postgresql://localhost/my_database_name"
	             }
```

You could then load the altered configuration by passing the `-c` option to your program.
Alternately, you could pass the configuration settings directly to the builder:

```

	Env env = new EnvBuilder()
	        .setProperty("dao.dataSource.default", "psql")
	        .setProperty("dao.dataSource.psql.username", "foo")
	        .setProperty("dao.dataSource.psql.password", "bar")
	        .setProperty("dao.dataSource.psql.url", "jdbc:postgresql://localhost/my_database_name")
	        .build();
```
 
