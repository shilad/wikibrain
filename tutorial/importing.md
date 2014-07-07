---
    layout: default
    title: Importing data
---
# Importing data

**Warning: This describes the operation of the PipelineLoader in the new better-pipeline branch.**

WikiBrain's PipelineLoader class manages the downloading, parsing, and import of [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.

Most commonly, PipelineLoader is run without any arguments except a language. This downloads, parses and imports the latest database files for the Simple English langauge edition of Wikipedia into an embedded h2 database:

```bash
org.wikibrain.Main -l simple
```

Each step in the WikiBrain loading process is called a **stage**. 
By default a set of `core` stages are loaded (articles, links, lucene search index), but others are not (wikidata, spatial).
You can specify the stages you want loaded, such as wikidata using the `-s` option:

```bash
org.wikibrain.Main -l simple -s wikidata
```

You may also want to configure how the stages you select are run.
For example, you can specify that each stage should incorporate a custom override configuration file:

```bash
org.wikibrain.Main -l simple -s wikidata -c custom.conf
```

These features are explained in more detail below.

## Stage selection

WikiBrain's import pipeline is divided into stages, listed in the table below.
If you don't tell WikiBrain to load specific stages, the stages marked `on = Y` are run.
The `concepts` stage is also run by default if more than one language is installed.

| on | stage      | dependsOn     |  description |
|----|------------|---------------|--------------|
| Y | fetchlinks | nothing       | Downloads a list of available data files in the requested languages. |
| Y | download   | fetchlinks    | Downloads any new data files in the requested languages. |
| Y | dumploader | download      | Loads articles and raw pages from the dump files. |
| Y | redirects  | dumploader    | Loads redirect mapping. |
| Y | wikitext   | redirects     | Loads links and categories. |
| Y | lucene     | wikitext      | Builds lucene full-text search indexes. |
| Y | phrases    | wikitext      | Builds and loads phrase to page disambiguation mappings. |
| ? | concepts   | redirects     | Builds and loads mapping from language-specific articles to universal concepts. |
| N | universal  | concepts, wikitext | Builds "universal" links. |
| N | wikidata   | concepts      | Downloads and loads wikidata facts for the requested languages. |
| N | spatial    | wikidata      | Installs geospatial data. |
| N | sr         | wikitext, phrases, lucene | Builds semantic relatedness models. |

You can run a specific stage using the `-s` flag. 
For example, the following command builds the semantic relatedness (SR) model and prepares it for use.

```bash
org.wikibrain.Main -s sr
```

You can specify multiple stages using multiple `-s` arguments:

```bash
org.wikibrain.Main -s sr -s spatial
```

If you ask WikiBrain to run a stage (e.g. `sr`), it checks to see if the stages it depends on (in this case, `wikitext`, `phrases`, `lucene`) have been run.
If necessary it runs them, and any further transitive dependencies.
You can disable checking to see if a stage is run, and rerun all necessary stages by using the `-d` dependency (for drop).


## Configuring stages

You can configure the global settings for all stages by passing any "standard" WikiBrain argument.
If any of these options exist, they will be passed along to all stages.

```text
 -c,--conf                  configuration file
 -h,--threads               the maximum number of threads that should be
                            used
 -l,--languages             the set of languages to process, separated by
                            commas or 'LOADED'
    --base-dir              the base directory used to resolve relative
                            directories
    --tmp-dir               the temporary directory
```



In addition, you can pass stage-specific arguments via the `-s` flag.
For example the following command runs the phrase stage (turns it `on`), and passes the argument `-p stanford` to it to tell it to load the [Stanford concept analyzer](http://googleresearch.blogspot.com/2012/05/from-words-to-concepts-and-back.html).

```
org.wikibrain.Main -s "phrases:on:-p stanford"
```

## More details

Each WikiBrain stage corresponds to a Java class with a `main()` method that can be run as a standalone Java program.
For example, the wikitext stages coresponds to [org.wikibrain.loader.WikiTextLoader](https://github.com/shilad/wikibrain/blob/master/wikibrain-loader/src/main/java/org/wikibrain/loader/WikiTextLoader.java).
You can find the full definition of stages at the end of the [reference.conf](https://github.com/shilad/wikibrain/blob/master/wikibrain-core/src/main/resources/reference.conf).
