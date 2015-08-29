---
    layout: default
    title: Web API
---
# Web API

WikiBrain includes a Web API that exposes simplified, developer-friendly versions of WikiBrain functions. 
As of September 2015, this API is in active development supported by the WikiMedia Foundation under the [WikiBrainTools IEG](https://meta.wikimedia.org/wiki/Grants:IEG/WikiBrainTools).

# Public endpoints

**No public endpoints are yet available for this endpoint.**

**Within Wikimedia Labs**, an endpoint is available  at:

* http://wikibrain0.eqiad.wmflabs:8080 for languages: en, de, pt, es, fr, it, ja, no
* Interested in other languages? Email Shilad at ssen@macalester.edu

# Structure of API requests:

All *requests* use simple URLs with query parameters:
    `http://endpoint/method?param1=val1&param2=val2...`.

Common request query parameters include:

* **Language**: For example, `lang=en`.
* **Single entities**: Requests related to single entities must contain exactly one of the following parameters:
  * `title`: The URL-encoded title of a Wikipedia article. For example, `title=Wikimedia%20Foundation`.
  * `articleId`: The id of the article. For example, `articleId=18618509`.
  * `conceptId`: The id of the associated Wikidata concept. For example, `conceptId=180` represents the [Wikimedia Foundation](https://www.wikidata.org/wiki/Q180).
  * `phrase`: The URL encoded textual phrase. For example, `phrase=The%20Wikimedia%20Foundation`. Note that this parameter cannot be used when the entity must be an article.
* **Multiple entities**: Requests about multiple entities contain plural versions of the above query parameters with an `s` added to the parameter name. Entities are separted by an or-bar (`|`). For example, `articleIds=18618509|5043734`.

# Structure of API responses:

API responses are formatted in JSON and encoded using UTF-8. Every JSON response includes two keys:

 * `success`: A boolean of true or false indicating whether the API call succeeded.
 * `message`: If the call does not succeed (`success` if `false`), message will contain an error message. Otherwise, it will contain contain the empty string.

JSON representation of articles share a common format containing "articleId", "lang", and "title" keys (as well as additional keys relevant to the particular API call). For example:

```json
{ "lang":"simple", "articleId":19903, "title":"Spider" }
```

# API Calls

###mostSimilar: 

Returns the most related articles for a particular article or phrase. 
Relatedness is defined using a semantic relatedness algorithm that is based on [Hecht et al's AtlasifySR+E](http://www.brenthecht.com/papers/bhecht_sigir2012_ExpSpatialization_SRplusE.pdf).


**Input parameters:** 

* A single entity (see definition above)
* `n`: An optional parameter specifying the number of results that should be returned.

**Output format:** 

* `results`: a list of articles ordered most to least related. Each article's JSON object includes a `score` attribute that indicates the relatedness score, between 0.0 and 1.0.

**Example:** 

http://localhost:8000/mostSimilar?lang=simple&phrase=spider?n=3

```json
{
    "success":true,
    "message":"",
    "results":[
        {"title":"Spider","score":0.9392013984939758,"lang":"simple","articleId":19903},
        {"title":"Arachnid","score":0.46658547513090154,"lang":"simple","articleId":22923},
        {"title":"Scorpion","score":0.4409242802398655,"lang":"simple","articleId":22045}
    ]
}
```

###similarity: 

Returns the relatedness score between two articles or phrases.
Relatedness is defined using a semantic relatedness algorithm that is based on [Hecht et al's AtlasifySR+E](http://www.brenthecht.com/papers/bhecht_sigir2012_ExpSpatialization_SRplusE.pdf).


**Input parameters:** 

* Two entities (see definition above)

**Output format:** 

* `entity1`: The first requested entity.
* `entity2`: The second requested entity.
* `score`: A relatedness score between 0.0 and 1.0.

**Example:** 

http://localhost:8000/similarity?lang=simple&phrases=coltrane|blues
```json
{
    "success":true,
    "message":"",
    "entity1":{"phrase":"coltrane","type":"phrase"},
    "entity2":{"phrase":"blues","type":"phrase"},
    "score":0.6527105891471714
}
```

###Cosimilarity

TODO

###Wikify

Identifies Wikipedia articles mentioned in a passage of free text.
Algorithm is based on 

http://localhost:8000/wikify?lang=simple&text=Wikipedia+is+a+free-access%2C+free-content+Internet+encyclopedia%2C+supported+and+hosted+by+the+non-profit+Wikimedia+Foundation

```json
{
    "success":true,
    "message":"",
    "text":"Wikipedia is a free-access, free-content Internet encyclopedia, supported and hosted by the non-profit Wikimedia Foundation.",
    "references": [
        {"title":"Wikipedia","text":"Wikipedia","index":0,"lang":"simple","articleId":27263},
        {"title":"Free content","text":"free-content","index":28,"lang":"simple","articleId":129718},
        {"title":"Internet","text":"Internet","index":41,"lang":"simple","articleId":362},
        {"title":"Encyclopedia","text":"encyclopedia","index":50,"lang":"simple","articleId":217},
        {"title":"Non-profit organization","text":"non-profit","index":92,"lang":"simple","articleId":83609},
        {"title":"Wikimedia Foundation","text":"Wikimedia Foundation","index":103,"lang":"simple","articleId":224356}
    ],
}
```


**pageRank**: Returns the pageRank of a particular article. The pageRank represents the "importance" in the link graph for a particular page. The sum of page ranks for all articles in a particular language equals 1.0. For example, the following call shows that the "United States" article in the Simple Wikipedia represents about 2.0% of the total page rank in the language.

http://localhost:8000/pageRank?title=United_States&lang=simple

```json
{
    "success":true,
    "message":"",
    "pageRank":0.019948040315214213,
    "article":{"title":"United States","lang":"simple","articleId":219587}
}
```

**articlesInCategory**: Given a category "c" and a set of candidate categories "S", returns all pages that are closer to c than S. If S is not specified, the language edition's top-level categories are used. S must contain c.

http://localhost:8000/articlesInCategory?targetCategoryTitle=Category:Science&lang=simple

```json
{
    "success":true,
    "message":"",
    "category":{"title":"Category:Science","lang":"simple","articleId":5833},"success":true,
    "distances":[
        {"distance":0.0,"title":"Process","lang":"simple","articleId":202920},
        {"distance":0.0,"title":"Applied science","lang":"simple","articleId":402104},
        {"distance":0.0,"title":"Scientific law","lang":"simple","articleId":171228},
        {"distance":0.0,"title":"Instrumentation","lang":"simple","articleId":144685},
        ...
    ],
}
```

**categoriesForArticle**: Given an article, returns the distance to each of a set of categories in in the category graph. If no categories are specified, top-level categories are used.

```json
{
    "success":true,
    "message":"",
    "article":{"title":"Jesus","type":"title","articleId":219585},
    "distances":[
        {"distance":0.33521585396335846,"title":"Category:Religion","lang":"simple","articleId":6106},
        {"distance":0.37135337094738713,"title":"Category:People","lang":"simple","articleId":5904},
        {"distance":0.7239022222538307,"title":"Category:Knowledge","lang":"simple","articleId":65947},
        {"distance":0.9894527716878347,"title":"Category:Science","lang":"simple","articleId":5833},
        {"distance":1.0924154851425356,"title":"Category:Geography","lang":"simple","articleId":5834},
        {"distance":1.095675386326904,"title":"Category:Everyday life","lang":"simple","articleId":5865}
    ]
}
```
