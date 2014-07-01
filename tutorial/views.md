---
    layout : default
    title : Page Views
---

# Page view module       
WikiBrain provides a way of downloading the number of pageviews for each article for a given hour. This data is published by the wikimedia foundation on dumps.wikimedia.org website. The number of page views for each article is used as a metric for populatity of articles. Once the requested hour(s) are loaded into the database several pieces of information can be accessed using PageViewSqlDao, including:

* `getNumViews(language, pageid, startDate, endDate)` returns the number of pageviews for the specified page ID in the language given for the hours that fall in between the start and end date.
* `getNumViews(Language language, int id, DateTime startDate, int numberOfHours, LocalPageDao localPageDao)` returns the number of pageviews for the specified pageviews for the specified page ID in the language given for the number of hours given after the start date.
* `getNumViews(Language lang, Iterable<Integer> ids, DateTime startTime, DateTime endTime, LocalPageDao localPageDao)` returns a map of the given page IDs to the number of pageviews in the language given for the times between the start and end date.
* `getAllViews(Language language, DateTime startDate, DateTime endDate, LocalPageDao localPageDao)` returns a map of all page IDs in the given language to pageviews from all hours between the start and end date.

To access this pageview information, you must first run PageViewLoader to download the desired hours. You must specify in your arguments what language you would like, and what hours you would like to download. The syntax for the command is language code first, followed by the start date, then the end date. The syntax for the dates is `<four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23>`. An example for downloading the pageviews for Simple English for the whole day of June 21, 2014 is as follows:

```bash
./wb-java.sh org.wikibrain.pageview.PageViewLoader simple 2014-6-21-0 2014-6-21-23
```
You must only download the pageviews for each hour once on each machine. If you call one of the methods from PageViewSqlDao with a date not yet downloaded, the class will automatically download the unacquired dates. However, we recommend you use PageViewLoader to download your dates in advance.

Once you have downloaded the needed pageview files, you can search for pageviews for specific articles with the code like the following:

```bash
        // Initialize the WikiBrain environment, get local page dao and pageview dao
        Env env= EnvBuilder.envFromArgs(args);
        Configurator configurator = env.getConfigurator();
        PageViewSqlDao pageViewSqlDao= configurator.get(PageViewSqlDao.class);
        LocalPageDao localPageDao = configurator.get(LocalPageDao.class);
        UniversalPageDao universalPageDao= configurator.get(UniversalPageDao.class);

        DateTime startTime= new DateTime(2014,6,21,0,0); //set the start date to year, month,day, hour, min
        DateTime endTime= new DateTime(2014,6,21,23,0);
            
        List<Integer> testList = new ArrayList();
        
        //adds local IDs to list to test pageview dao
        testList.add(219587);
        testList.add(47);
        testList.add(39);
        testList.add(10983);
        
        //calls pageview dao and maps local IDs from testList to num views
        Map<Integer,Integer> results=pageViewSqlDao.getNumViews(Language.SIMPLE,testList,startTime,endTime,localPageDao);
        
        //prints all pages and pageviews from testList
        for (Integer id : results.keySet()) {
            System.out.println(id + " :: " + localPageDao.getById(Language.SIMPLE, id).getTitle().toString() + " :: " + results.get(id));
        }
```

When run, this code prints out the following:

```bash
    39 :: Apple (simple) :: 61
    858 :: Universe (simple) :: 47
    219587 :: United States (simple) :: 153
    47 :: Atom (simple) :: 96
    10983 :: Minnesota (simple) :: 13
```

The first number is the local ID (in Simple English) of the concept, followed by the title of the page, and the final number is the number of pageviews. All of this is done in Simple English. For example, the Simple English "Apple" page received 61 pageviews on June 21, 2014, while "United States" received 153.
