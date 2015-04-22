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
'Barack Obama' is similar to 1) Barack Obama (simple), 2) Woodrow Wilson (simple), 3) Theodore Roosevelt (simple)
'US' is similar to 1) United States (simple), 2) United States Virgin Islands (simple), 3) Puerto Rico (simple)
'Canada' is similar to 1) Canada (simple), 2) British Virgin Islands (simple), 3) Grenada (simple)
'vim' is similar to 1) Vim (simple), 2) GNU Emacs (simple), 3) Text editor (simple)
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
0.9816753364801072: 'cat', 'kitty'
0.6306937386958802: 'Obama', 'president'
0.6989272555126909: 'tires', 'car'
0.4818681937945792: 'java', 'computer'
0.47369101672011227: 'dog', 'computer'
```
