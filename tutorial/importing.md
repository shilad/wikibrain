---
    layout: default
    title: Importing data
---
# Importing data

It automatically downloads and organizes [Wikipedia datasets](http://en.wikipedia.org/wiki/Wikipedia:Database_download) published by the Wikimedia foundation.

Download and process the dataset:

```bash
wb-java.sh org.wikibrain.dao.load.PipelineLoader -l simple
```


The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. It imports the data into an embedded h2 database. You can customize WikiBrain's importing procedure, (see Configuration, below) but the default should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

```bash
wb-java.sh org.wikibrain.dao.load.PipelineLoader -l en,fr
``` 
(beware that this is a lot of data and takes many hours!).