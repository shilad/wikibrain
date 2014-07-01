---
    layout: default
    title: Importing data
---
# Importing data

**Warning: This describes the operation of the PipelineLoader in the new better-pipeline branch.**

WikiBrain includes a Java program called PipelineLoader that manages the downloading, parsing, and import of data files.

Most commonly, you will run PipelineLoader without any arguments:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple
```
The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. It imports the data into an embedded h2 database. You can customize WikiBrain's importing procedure, (see Configuration, below) but the default should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

You may want to customize the PipelineLoader's behavior by selecting what data to import. 
Each step in the WikiBrain loading process is called a **stage**. 
Some stages are loaded by default (articles, links, lucene search index), but others are not (wikidata, spatial).
You can turn on a stage, such as wikidata by using the following:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple -g wikidata
```

You may also want to configure how each stage is run.
For example, you can specify that each stage should incorporate a custom override configuration file:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple -g wikidata -c custom.conf
```

These features are explained in more detail below.

## Stage configuration

It automatically downloads and organizes [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.

Download and process the dataset:




```bash
wb-java.sh org.wikibrain.dao.load.PipelineLoader -l en,fr
``` 
(beware that this is a lot of data and takes many hours!).
