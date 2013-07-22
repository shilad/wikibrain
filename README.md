wikAPIdia
=====
An API for accessing and organizing Wikipedia's language dumps. Specifically we:
* Support multiple languages in parallel.
* Resolve all redirected links to the page they point to.
* Document all links out from a given Wikipedia page.
* Document all of the members of a specific Wikipedia category.
* Overlay concepts that are cross-lingual and links between concepts.

This API is meant to be used for anyone wishing to manipulate Wikipedia's vast information.

Software and Hardware Recommendations
-----------
* Maven (required)
* Bash (required)
* One of the following <ul> <li>md5sum</li> <li>md5</li> <li>sum</li></ul>
* Hardware stuff (to be filled out)

User Instructions
-----------
* Universal parameters for all scripts:

```bash
[-c conf] [-l languages...] [-h threads]
```
```bash
-c      Sets configuration file to the specified path
-l      Selects languages by language code to retrieve from wikimedia, separated by commas
-h      Sets the maximum amount of processors to use for parallel language processing
```

* Run the requestedlinkgetter.sh file. The parameters should be formatted to match the following:

```bash
[-o outputpath] [-n names...] [-d date]
```
```bash
-o      Sets the path to output the tsv file containing all the links
-n      Selects types of dump files to retrieve, separated by commas
-d      Sets the date to retrieve from. Files are retrieve from on or before this date
```

* Run the filedownloader.sh file. The parameters should be formatted to match the following:

```bash
[-o outputpath] [-t tsvpath]
```
```bash
-o      Sets the directory in which to output the downloaded dumps
-t      Selects the tsv file from which to read the download links
```

* Run the dumploader.sh file. The parameters should be formatted to match the following:

```bash
[file ...]
```
```bash
file    Selects the dump files to load
```
	
* Run the redirectloader.sh file. The parameters should be formatted to match the following:

```bash
[-d]
```
```bash
-d      Drops and recreates all tables and indexes
```
 	
* Run the wikitextdumploader.sh file. The parameters should be formatted to match the following:

```bash
[-d]
```
```bash
-d      Drops and recreates all tables and indexes
```

* Run conceptmapper.sh file. The parameters should be formatted to match the following:

```bash
[-d] [-n algorithms]
```
```bash
-d      Drops and recreates all tables and indexes
-n      Selects the algorithms to use to map concepts
```

* Run universallinkloader.sh. The parameters should be formatted to match the following:

```bash
[-d] [-n algorithms]
```
```bash
-d      Drops and recreates all tables and indexes
-n      Selects the algorithms to use to map concepts
```

Optional scripts:

* Run phraseloader.sh. The parameters should be formatted to match the following:

```bash
[-n analyzer]
```
```bash
-n      Selects the phrase analyzer to use
```

* Run luceneloader.sh. The parameters should be formatted to match the following:

```bash
[-d] [-n namespace...] [-i index...]
```
```bash
-d      Drops and recreates all Lucene indexes
-n      Specifies the namespaces to indexes
-i      Selects the types of indexes to use, as described by the configuration file
```
 
A Basic Outline of the Process 
-----------
* Download dump files <ul><li>Obtain dump links</li> <li>Download files from those links</li></ul>
* Load the Dump as XML <ul><li>Convert Dump into RawPages </li> <li>Convert RawPages into LocalPages <ul>
					<li>Mark Redirects to be dealt with after this process</li> </ul></li>
			</ul>
* Resolve Redirects <ul><li>Load into Redirect Table, fully resolved </li></ul>
* WikiTextParser does the following <ul><li>load links into table with src/dest IDs </li> <li>load categories with the source article as a category member</li></ul>
* Load Concepts
* Load Concept Links

Optional:
* Load Phrases Database
* Load Lucene Database
