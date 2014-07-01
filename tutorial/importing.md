---
    layout: default
    title: Importing data
---
# Importing data

**Warning: This describes the operation of the PipelineLoader in the new better-pipeline branch.**

WikiBrain's PipelineLoader class manages the downloading, parsing, and import of [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.

Most commonly, PipelineLoader is run without any arguments except a language:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple
```
This downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. It imports the data into an embedded h2 database. 

You may want to select what data PipelineLoade should import. 
Each step in the WikiBrain loading process is called a **stage**. 
Some stages are loaded by default (articles, links, lucene search index), but others are not (wikidata, spatial).
You can turn on a stage, such as wikidata using the following:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple -g wikidata
```

You may also want to configure how the stages you select are run.
For example, you can specify that each stage should incorporate a custom override configuration file:

```bash
org.wikibrain.dao.load.PipelineLoader -l simple -g wikidata -c custom.conf
```

These features are explained in more detail below.

## Stage selection

WikiBrain provides a variety of stages. These are generally organized into **groups.**

 * **core:**
 * **multilingual:**
 * **wikidata:**
 * **spatial:**


| stage | group | class | description |
|-------|-------|-------|-------------|
