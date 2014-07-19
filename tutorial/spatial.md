---
    layout: default
    title: Spatial
---
        
# Spatial
To intialize the spatial data, you should have [PostGIS](http://postgis.net/install) installed. The library was tested on Postgres 9.3.4 with PostGIS 2.12 on Mac OS X. After installing PostGIS, create a new database, connect to the new database and run the following SQL to enable spatial support:

```text
-- Enable PostGIS (includes raster)
CREATE EXTENSION postgis;
```
Then, go to the [reference.conf](wikibrain-core/src/main/resources/reference.conf) and configure the following settings corresponding to your PostGIS settings.

```text
spatial : {

    dao : {

        dataSource : {

                // These all use keys standard to Geotools JDBC
                // see: http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html

                #change this part according to your DB settings
                default : postgis
                postgis : {
                    dbtype : postgis
                    host : localhost
                    port : 5432
                    schema : public
                    database : wikibrain
                    user : toby
                    passwd : ""
                    max connections : 19
                }
            }
```

Loading the Wikidata layer in the spatial module also requires having Wikidata loaded (see the Wikidata section of this README file)

Now you can load the Wikidata layer by running:

```bash
./wb-java.sh org.wikibrain.spatial.loader.SpatialDataLoader
```

Try running [CalculateGeographicDistanceBetweenPages](/wikibrain-spatial/src/main/java/org/wikibrain/spatial/cookbook/CalculateGeographicDistanceBetweenPages.java). If it runs correctly, the spatial module is successfully initialized.

## Integrating new layers with WikiBrain (or updating existing integrated layers)

1. Make sure the dataset information is correct in the reference.conf's spatial.datasets configuration.
2. Run the ShapeFileMatcher with the name of the reference system, layer, and datasetname: 

```bash
org.wikibrain.spatial.matcher.ShapeFileMatcher earth country naturalEarth
```

3. Go to the `dat/spatial/<refSys>/<layerGroup>/` directory, and edit the csv file called `<datasetName>.wbmapping.csv`. You can take a look at an example for natural earth countries in this [Google Spreadsheet](https://docs.google.com/a/macalester.edu/spreadsheets/d/1woZTOhw_as6L_Mt-xGxg28jdGYXF-mJlGlzSN5twffE/edit#gid=1543775261). The important columns are as follows:

  *   WB_STATUS: Starts as "U" (unknown) when automatically matched. Change to "V" (verified) after you are certain the matching is correct.
  *   WB_TITLE: should contain the title of the correct Wikipedia article. Defaults to WikiBrain's best guess.
  *   WB_GUESS1 / WB_GUESS2 / WB_GUESS3: Alternate guesses about the title.
  *   WB_SCORE: A score indicating WikiBrain's confidence in its match.

You should only change WB_STATUS and WB_TITLE. You can also reorder the spreadsheet rows.

4. 
