---
    layout: default
    title: Configuration
---
        

# Configuration

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
 