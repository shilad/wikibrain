wikAPIdia
=====
WikAPIdia is a multi-lingual Java framework that provides easy and efficient access to Wikipedia data. Specifically we:
* Offer convient tools for downloading and organizing Wikipedia data.
* Support multi-lingual data analysis that analyzes relationships between multiple language editions of Wikipedia.
* Provide semantic-relatedness algorithms that measure the relationship between two concepts such as "racecar" and "engine."

###System Requirements
* Maven (required)
* Bash (required)
* A clone of this repository

###Importing data into WikAPIdia

* Clone this repository ```git-clone https://github.com/shilad/wikAPIdia.git```
* Download and process the dataset:

```bash
	cd wikAPIdia
	cd wikAPIdia-parent
	./scripts/run-pipeline all -l simple
```

The last command downloads, installs, and analyzes the latest database files for the Simple English langauge edition of Wikipedia. 

You can customize WikAPIdia's importing procedure, but the run-pipeline-sh script should be a good start. For example, you can specify different language editions by changing the -l parameters. To analyze English and French you could run: 

```bash
./scripts/run-pipeline all -l en,fr
``` 
(beware that this is a lot of data!).


###Writing Java programs that use the WikAPIdia framework to analyze data
Once you have imported data (above), your are ready to write programs that analyze Wikipedia!
Here's a [simple example](https://github.com/shilad/wikAPIdia/blob/master/wikAPIdia-cookbook/src/main/java/org/wikapidia/phrases/cookbook/ResolveExample.java) you can find in the Cookbook:

```java
// Prepare the environment; set the root to the current directory (".").
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();

// Get the configurator that creates components and a phraze analyzer from it 
Configurator configurator = env.getConfigurator();
PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class);

// get the most common phrases in simple  
Language simple = Language.getByLangCode("simple");   // simple english 
LinkedHashMap<LocalPage, Float> resolution = pa.resolveLocal(simple, "apple", 5);
        
// show the closest pages
System.out.println("resolution of apple");
if (resolution == null) { 
    System.out.println("\tno resolution !");
} else {
    for (LocalPage p : resolution.keySet()) {
        System.out.println("\t" + p + ": " + resolution.get(p));
    }       
} 
```

When you run this program, you'll see output:

```txt
resolution of apple
	LocalPage{nameSpace=ARTICLE, title=Apple, localId=39, language=Simple English}: 0.070175424
	LocalPage{nameSpace=ARTICLE, title=Apple juice, localId=19351, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Macintosh, localId=517, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple Inc., localId=7111, language=Simple English}: 0.043859642
	LocalPage{nameSpace=ARTICLE, title=Apple A4, localId=251288, language=Simple English}: 0.043859642
```

Let's walk through this program to explain each piece. 
First, we create an ```Env``, a WikAPIdia environment that provides access to the components we need:
```java
Env env = new EnvBuilder()
        .setBaseDir(".")
        .build();
```


