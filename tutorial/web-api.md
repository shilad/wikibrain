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

* http://wikibrain0.eqiad.wmflabs for languages: en, de, pt, es, fr, it, ja, no
* Email shilad for other languages: ssen@macalester.edu

# Structure of API requests:

All *requests* use simple URLs with query parameters:
    `http://endpoint/method?param1=val1&param2=val2...`.

Common request query parameters include:

* Language: for example, `lang=en`.
* Requests about single "entities", which must contain exactly one of the following parameters:
  * `title`: The title of a Wikipedia article, e.g. `title=Wikimedia%20Foundation`
  * `articleId`: The id of the article, e.g. `articleId=18618509`
  * `conceptId`: The id of the associated Wikidata concept, e.g. `conceptId=180`
  * `phrase`: A textual phrase, e.g. `phrase=The%20Wikimedia%20Foundation`
* Requests about multiple entities contain plural versions of the above query parameters (an `s` is added), with individual entities delimited by an or-bar (`|`), e.g. `articleIds=18618509|5043734`

# Structure of API responses:

API responses are formatted in JSON.



