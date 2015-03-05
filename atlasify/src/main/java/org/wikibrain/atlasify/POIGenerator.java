package org.wikibrain.atlasify;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by toby on 3/4/15.
 */
public class POIGenerator {
    private static Map<Integer, Geometry> geometryMap = null;
    private static SpatialDataDao sdDao = null;

    POIGenerator(Configurator conf) throws DaoException, ConfigurationException{
        sdDao = conf.get(SpatialDataDao.class);
        geometryMap = sdDao.getAllGeometriesInLayer("wikidata");
    }
    //Return POIs that have direct link to the keyword
    public String getDirectedLinkedPOI(String keyword, AtlasifyResource atlasifyResource) throws DaoException{

        LocalId queryID=new LocalId(atlasifyResource.lang,0);
        try{
            queryID=atlasifyResource.wikibrainPhaseResolution(keyword);
        }
        catch(Exception e){
            System.out.println("Failed to resolve keyword "+keyword);
            return "";
        }
        Map<String, Point> resultMap = new HashMap<String, Point>();

        Iterable<LocalLink> outlinks = atlasifyResource.llDao.getLinks(atlasifyResource.lang, queryID.getId(), true);
        Iterable<LocalLink> inlinks = atlasifyResource.llDao.getLinks(atlasifyResource.lang, queryID.getId(), false);



        return "";
    }



    //Return POIs in TopN algorithm
    public String getTopNPOI(String keyword, AtlasifyResource atlasifyResource) throws SchemaException, IOException{
        Map<String, String>srMap=new HashMap<String, String>();
        LocalId queryID=new LocalId(atlasifyResource.lang,0);
        try{
            queryID=atlasifyResource.wikibrainPhaseResolution(keyword);
        }
        catch(Exception e){
            System.out.println("Failed to resolve keyword "+keyword);
            return "";
        }
        // LocalId queryID = new LocalId(Language.EN, 19908980);
        Map<String, Point>resultMap=new HashMap<String, Point>();
        try{
            Map<LocalId, Double>srValues=atlasifyResource.accessNorthwesternAPI(queryID, 400);
            for(Map.Entry<LocalId, Double>e:srValues.entrySet()){
                try{
                    LocalPage localPage=atlasifyResource.lpDao.getById(e.getKey());
                    int univId=atlasifyResource.upDao.getByLocalPage(localPage).getUnivId();
                    if(geometryMap.containsKey(univId)){
                        resultMap.put(localPage.getTitle().getCanonicalTitle(),geometryMap.get(univId).getCentroid());
                    }
                }
                catch(Exception e1){
                    continue;
                }

            }

        }
        catch(Exception e){
            System.out.println("Error when connecting to Northwestern Server ");
            e.printStackTrace();
            // do nothing

        }

        return geoJSONPacking(resultMap);

    }

    private String geoJSONPacking(Map<String, Point> resultMap) throws IOException, SchemaException{
        FeatureJSON featureJSON = new FeatureJSON();
        SimpleFeatureType featureType= DataUtilities.createType("TYPE", "geometry:Point,name:String");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
        for(Map.Entry<String, Point> entry: resultMap.entrySet()){
            fb.add(entry.getValue());
            fb.add(entry.getKey());
            SimpleFeature feature = fb.buildFeature(null);
            featureList.add(feature);
        }
        SimpleFeatureCollection featureCollection = DataUtilities.collection(featureList);
        String jsonResult = featureJSON.toString(featureCollection);
        return jsonResult;
    }

}
