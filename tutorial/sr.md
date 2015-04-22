---
    layout: default
    title: Semantic Relatedness
---
        
# Semantic relatedness algorithms
WikiBrain provides several state-of-the-art semantic relatedness algorithms (*SR metrics*). These algorithms estimate the strength of semantic relationships between concepts. 
These algorithms are designed to be fast, with performance of 10-100 milliseconds and caching and multi-threaded support built in.
WikiBrain SR metrics support six major functions:

* `similarity(phrase1, phrase2)` returns the relatedness score between two phrases.
* `similarity(page1, page2)` returns the relatedness score between two pages.
* `mostSimilar(phrase)` returns the most similar phrases to a particular target phrase.
* `mostSimilar(page)` returns the most similar pages to a particular target page.
* `cosimilarity(rowPhrases[], colPhrases[])` computes a cosimilarity matrix for the specified row and column phrases.
* `cosimilarity(rowPages[], colPages[])` computes a cosimilarity matrix for the specified rows and column pages.

There are a variety of different SR metrics built into WikiBrain. 
Each  metric offers tradeoffs in offline build time, online runtime, memory usage, coverage, accuracy etc.
By default, WikiBrain builds Milne and Witten's [link-based algorithm](http://www.aaai.org/Papers/Workshops/2008/WS-08-15/WS08-15-005.pdf) (2006) a fast, releatively accurate algorithm.
More details on this and other algorithms appear at the end of this page.

After the SR algorithm is trained, you can use it in your Java application. For example, to use the `mostSimilar()` method for phrases, do the following:

```java    
// Initialize the WikiBrain environment and get the local page dao
Env env = new EnvBuilder().build();
Configurator conf = env.getConfigurator();
LocalPageDao lpDao = conf.get(LocalPageDao.class);
Language simple = Language.getByLangCode("simple");

// Retrieve the "ensemble" sr metric for simple english 
SRMetric sr = conf.get(
        SRMetric.class, "milnewitten",
        "language", simple.getLangCode());

//Similarity between strings 
for (String phrase : Arrays.asList("Barack Obama", "US", "Canada", "vim")) {
    SRResultList similar = sr.mostSimilar(phrase, 3);
    List<String> pages = new ArrayList<String>();
    for (int i = 0; i < similar.numDocs(); i++) {
        LocalPage page = lpDao.getById(simple, similar.getId(i));
        pages.add((i+1) + ") " + page.getTitle());
    }       
    System.out.println("'" + phrase + "' is similar to " + StringUtils.join(pages, ", ")); 
}  
```

This code (on Simple english) displays:

```
*Barack Obama* is similar to 1) Barack Obama, 2) Hillary Rodham Clinton, 3) Mitt Romney
*US* is similar to 1) United States, 2) Federal government of the United States, 3) United States Constitution
*Canada* is similar to 1) Canada, 2) Quebec, 3) Australia
*vim* is similar to 1) Vim, 2) Text editor, 3) GNU Emacs
```

You can also calculate a relatedness score between any two pages or phrases using the same SRMetric:

```java
//Similarity between strings 
String pairs[][] = new String[][] {
        { "cat", "kitty" },
        { "obama", "president" },
        { "tires", "car" },
        { "java", "computer" },
        { "dog", "computer" },
};      

for (String pair[] : pairs) {
    SRResult s = sr.similarity(pair[0], pair[1], false); 
    System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'"); 
}    
```

This code (on Simple english) displays:

```
0.7652857508808703: 'cat', 'kitty'
0.6816860201660842: 'obama', 'president'
0.7058954876236786: 'tires', 'car'
0.5905978329192705: 'java', 'computer'
0.42989849626985877: 'dog', 'computer'
```

## Other SR algorithms:


To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *inlink* metric):

```bash
./wb-java.sh org.wikibrain.sr.SRBuilder -m inlink
```

The inlink metric is a fast but relatively inaccurate SR metric. You can also build the "ensemble" metric that provides a linear combination of four other metrics. Beware that training the ensemble is costly. It takes about 10 minutes on Simple English Wikipedia, and a little over a day on the full Wikipedia. Most of the model-building time supports the *mostSimilar()* call, so you can speed up model building if you only need *similarity()*. TODO: explain how to do this.
