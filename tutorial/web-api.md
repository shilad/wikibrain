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
  * `title`: The title of a Wikipedia article, e.g. `title=Wikimedia%20Foundation`.
  * `articleId`: The id of the article, e.g. `articleId=18618509`.
  * `conceptId`: The id of the associated Wikidata concept, e.g. `conceptId=180`.
  * `phrase`: A textual phrase, e.g. `phrase=The%20Wikimedia%20Foundation`.
* **Multiple entities**: Requests about multiple entities contain plural versions of the above query parameters with an `s` added to the parameter name. Entities are separted by an or-bar (`|`). For example, `articleIds=18618509|5043734`.

# Structure of API responses:

API responses are formatted in JSON and encoded using UTF-8. Every JSON response includes two keys:

 * `success`: A boolean of true or false indicating whether the API call succeeded.
 * `message`: If the call does not succeed (`success` if `false`), message will contain an error message. Otherwise, it will contain contain the empty string.


