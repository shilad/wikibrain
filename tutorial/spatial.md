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

