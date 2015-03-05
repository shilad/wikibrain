package org.wikibrain.atlasify;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

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
    public String getDirectedLinkedPOI(String keyword, AtlasifyResource atlasifyResource) throws DaoException, WikiBrainException, SchemaException, IOException{

        LocalId queryID=new LocalId(atlasifyResource.lang,0);
        try{
            queryID=atlasifyResource.wikibrainPhaseResolution(keyword);
        }
        catch(Exception e){
            System.out.println("Failed to resolve keyword "+keyword);
            return "";
        }
        Map<Integer, Point> idGeomMap = new HashMap<Integer, Point>();
        Map<Integer, String> idTitleMap = new HashMap<Integer, String>();
        Map<Integer, String> idExplanationMap = new HashMap<Integer, String>();

        try {
            Iterable<LocalLink> outlinks = atlasifyResource.llDao.getLinks(atlasifyResource.lang, queryID.getId(), true);
            Iterable<LocalLink> inlinks = atlasifyResource.llDao.getLinks(atlasifyResource.lang, queryID.getId(), false);
            Iterator<LocalLink> outlinkIter = outlinks.iterator();
            Iterator<LocalLink> inlinkIter = inlinks.iterator();
            while(outlinkIter.hasNext()){
                LocalLink link = outlinkIter.next();
                int localId = link.getDestId();
                try{
                    int univId = atlasifyResource.upDao.getByLocalPage(atlasifyResource.lpDao.getById(atlasifyResource.lang, localId)).getUnivId();
                    if(geometryMap.containsKey(univId)){
                        idGeomMap.put(univId, geometryMap.get(univId).getCentroid());
                        idTitleMap.put(univId, atlasifyResource.upDao.getById(univId).getBestEnglishTitle(atlasifyResource.lpDao, true).getCanonicalTitle());
                        idExplanationMap.put(univId, keyword + " has a link to " + atlasifyResource.upDao.getById(univId).getBestEnglishTitle(atlasifyResource.lpDao, true).getCanonicalTitle());
                    }
                }
                catch (Exception e){
                    //do nothing
                    continue;
                }
            }

            while(inlinkIter.hasNext()){
                LocalLink link = inlinkIter.next();
                int localId = link.getSourceId();
                try{
                    int univId = atlasifyResource.upDao.getByLocalPage(atlasifyResource.lpDao.getById(atlasifyResource.lang, localId)).getUnivId();
                    if(geometryMap.containsKey(univId)){
                        idGeomMap.put(univId, geometryMap.get(univId).getCentroid());
                        idTitleMap.put(univId, atlasifyResource.upDao.getById(univId).getBestEnglishTitle(atlasifyResource.lpDao, true).getCanonicalTitle());
                        idExplanationMap.put(univId, atlasifyResource.upDao.getById(univId).getBestEnglishTitle(atlasifyResource.lpDao, true).getCanonicalTitle() + " : " + keyword + " is linked from " + atlasifyResource.upDao.getById(univId).getBestEnglishTitle(atlasifyResource.lpDao, true).getCanonicalTitle());
                    }
                }
                catch (Exception e){
                    //do nothing
                    continue;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return "";
        }


        return geoJSONPacking(idGeomMap, idTitleMap, idExplanationMap);

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
        Map<Integer, Point> idGeomMap = new HashMap<Integer, Point>();
        Map<Integer, String> idTitleMap = new HashMap<Integer, String>();
        Map<Integer, String> idExplanationMap = new HashMap<Integer, String>();
        try{
            Map<LocalId, Double>srValues=atlasifyResource.accessNorthwesternAPI(queryID, 400);
            for(Map.Entry<LocalId, Double>e:srValues.entrySet()){
                try{
                    LocalPage localPage=atlasifyResource.lpDao.getById(e.getKey());
                    int univId=atlasifyResource.upDao.getByLocalPage(localPage).getUnivId();
                    if(geometryMap.containsKey(univId)){
                        idGeomMap.put(univId, geometryMap.get(univId).getCentroid());
                        idTitleMap.put(univId, localPage.getTitle().getCanonicalTitle());
                        idExplanationMap.put(univId, localPage.getTitle().getCanonicalTitle() + " : " + localPage.getTitle().getCanonicalTitle() + " is a top related article to " + keyword);
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
        //System.out.println("START PACKING JSON FOR POI REQUEST " + keyword + " MAP SIZE " + idGeomMap.size() + " " + idTitleMap.size() + " " + idExplanationMap.size());
        String result = "";
        try{
            result = geoJSONPacking(idGeomMap, idTitleMap, idExplanationMap);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;

    }
    private String geoJSONPacking(Map<Integer, Point> idGeomMap, Map<Integer, String> idTitleMap, Map<Integer, String> idExplanationMap) throws IOException, SchemaException{
        FeatureJSON featureJSON = new FeatureJSON();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        typeBuilder.setName("Atlasify_POI");
        typeBuilder.add("geometry", Point.class);
        typeBuilder.add("name", String.class);
        typeBuilder.add("explanation", String.class);
        SimpleFeatureType featureType= typeBuilder.buildFeatureType();
        //System.out.println("FINISHED BUILDING FEATURETYPE");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(featureType);
        List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
        for(Map.Entry<Integer, Point> entry: idGeomMap.entrySet()){
            fb.set("geometry", entry.getValue());
            fb.set("name", idTitleMap.get(entry.getKey()));
            fb.set("explanation", idExplanationMap.get(entry.getKey()));
            SimpleFeature feature = fb.buildFeature(null);
            featureList.add(feature);
        }
        SimpleFeatureCollection featureCollection = DataUtilities.collection(featureList);
        String jsonResult = featureJSON.toString(featureCollection);
        return jsonResult;
    }

}
