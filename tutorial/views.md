---
    layout : default
    title : Page Views
---

# Page view module       
WikiBrain allows programs to load and query the number of pageviews for each article with hourly resolution.
The raw pageview data that WikiBrain uses was originally created by Domas Mituzas and is available at http://dumps.wikimedia.org/other/pagecounts-raw/.


## Importing pageviews
To import the pageview data, you must ensure that pageviews are loaded into the database.
The pageview datafiles are about 100MB per hour, so they take some time to download and space to store.
The following code would load two hours of data (about 200MB of downloaded data).

```java
// Get the pageview dao
Env env = EnvBuilder.envFromArgs(args);
PageViewDao viewDao = env.getConfigurator().get(PageViewDao.class);

// Download and import pageview stats if necessary
DateTime start = new DateTime(2014, 8, 14, 21, 0, 0);
DateTime end = new DateTime(2014, 8, 14, 22, 0, 0);
viewDao.ensureLoaded(start, end,  env.getLanguages());
```

## Querying pageviews for specific articles

Once the requested hour(s) are loaded into the database, they can be queried.
WikiBrain allows you to query the number of page views for a specific page, collection of pages, or all pages.

For example, you can find the pageviews for a few well-known musicians as follows:

```java
// Retrieve and display pageviews for specific pages
LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
Language lang = env.getDefaultLanguage();
System.out.println("Number of page views for specific pages:");
for (String title : Arrays.asList("Mozart", "Justin Bieber")) {
    int id = pageDao.getIdByTitle(title, lang, NameSpace.ARTICLE);
    int views = viewDao.getNumViews(new LocalId(lang, id), start, end);
    System.out.format("\t%s (id=%d) is %d\n", title, id, views);
}
```

For a two hours worth of data on Simple English Wikipedia (relatively low traffic), this yields:

```
Number of page views for specific pages:
	Mozart (id=4462) is 0
	Justin Bieber (id=201694) is 5
```

## Querying pageviews for all articles in a language

If you wanted to query pageview counts for all articles in a language, you should use a different API call that is more efficient:

```java
// Retrieve counts for all pageviews
TIntIntMap allViews = viewDao.getAllViews(lang, start, end);
int pageIds[] = WpCollectionUtils.sortMapKeys(allViews, true);
System.out.println("Top pageviews in " + lang);
for (int i = 0; i < 10; i++) {
    LocalPage page = pageDao.getById(lang, pageIds[i]);
    int n = allViews.get(pageIds[i]);
    System.out.format("%d. %s (nviews=%d)\n", (i+1), page.getTitle(), n);
}
```

On Simple English, this returns:

```
Top pageviews in Simple English
1. Main Page (simple) (nviews=911)
2. Brian Blessed (simple) (nviews=203)
3. List of Norse gods and goddesses (simple) (nviews=131)
4. List of U.S. states (simple) (nviews=130)
5. Search engine (simple) (nviews=117)
6. Heroin (simple) (nviews=108)
7. List of Egyptian gods and goddesses (simple) (nviews=89)
8. List of fruits (simple) (nviews=83)
9. The Dark Knight Trilogy (simple) (nviews=77)
10. List of cities in France (simple) (nviews=73)
```

## Words of warning

Pageviews require a substantial amount of data.
Regardless of what languages you have installed, the pageview logs are about 100MB per hour. 
A full day's views requires 2.5GB.

In addition, the pageview data is not (yet) aggregated in the database. 
This means that if you need to load several days of data for Full English Wikipedia you'll end up inserting about half a billion SQL records.

Finally, note that for every call to ensureLoaded that asks WikiBrain to load new data, the SQL indexes for the pageview table are dropped and recreated. Thus, you're better off calling ensureLoaded once for all the time intervals you need.
