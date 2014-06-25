---
    layout : default
    title : Page Views
---

# Page view module       
WikiBrain provides a way of downloading the number of pageviews for each article for a given hour. This data is published by the wikimedia foundation on dumps.wikimedia.org website. The number of page views for each article is used as a metric for populatity of articles. Once the requested hour(s) are loaded into the database several pieces of information can be accessed using PageViewSQLDao, including:

* `getNumViews(language, pageid, startDate, endDate)` returns the number of pageviews for the specified page ID in the language given for the hours that fall in between the start and end date.
* `getNumViews(Language language, int id, DateTime startDate, int numberOfHours, LocalPageDao localPageDao)` returns the number of pageviews for the specified pageviews for the specified page ID in the language given for the number of hours given after the start date.
* `getNumViews(Language lang, Iterable<Integer> ids, DateTime startTime, DateTime endTime, LocalPageDao localPageDao)` returns a map of the given page IDs to the number of pageviews in the language given for the times between the start and end date.
* `getAllViews(Language language, DateTime startDate, DateTime endDate, LocalPageDao localPageDao)` returns a map of all page IDs in the given language to pageviews from all hours between the start and end date.

To access this pageview information, you must first run PageViewLoader to download the desired hours. You must specify in your arguments what language you would like, and what hours you would like to download. The syntax for the command is language code first, followed by the start date, then the end date. The syntax for the dates is `<four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23>`. An example for downloading the pageviews for Simple English for the whole day of June 21, 2014 is as follows:

```bash
./wb-java.sh org.wikibrain.pageview.PageViewLoader simple 2014-6-21-0 2014-6-21-23
```
You must only download the pageviews for each hour once on each machine.
