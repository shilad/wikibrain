package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;

import java.util.*;

/**
 * Created by vonea001 on 7/11/14.
 */
public class MapViewer {

    private final Map<Integer,Geometry> geometries;
    private final List<Integer> pageHitList;
    private final Language language;

    public MapViewer(Configurator c){
        SpatialDataDao sdDao = null;
        Language lan = null;
        try{
            sdDao= c.get(SpatialDataDao.class);
            lan = Language.getByLangCode(c.get(LanguageSet.class).getLangCodes().get(0));

        }catch(ConfigurationException e){
            e.printStackTrace();
        }
        language = lan;
        // eventually, do something to geometries to make it have only significant entries
        Map<Integer,Geometry> geo = null;
        try {
            geo = sdDao.getAllGeometriesInLayer("significant", "earth");
        }catch(DaoException e){
            e.printStackTrace();
        }
        this.geometries = geo;
        pageHitList = new ArrayList<Integer>();
        pageHitList.addAll(geometries.keySet());
    }


    public static void main (String[] args){
        Env env = null;
        try{
            env = EnvBuilder.envFromArgs(args);
        }catch(ConfigurationException e){
            e.printStackTrace();
        }

        MapViewer mv = new MapViewer(env.getConfigurator());

        final MatrixGenerator.MatrixWithHeader matrix = MatrixGenerator.loadMatrixFile("graphmatrix_"+mv.language.getLangCode());

        // Generate highly assymetric pairs
//        List<Integer> sortedIds = new ArrayList<Integer>();
//        sortedIds.addAll(matrix.idsInOrder);
//        final int la = matrix.idsInOrder.indexOf(11299);//11299);//65);
//        Collections.sort(sortedIds,new Comparator<Integer>() {
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                int i=matrix.idsInOrder.indexOf(o1);
//                int j=matrix.idsInOrder.indexOf(o2);
//                float[] values = {matrix.matrix[la][i],matrix.matrix[i][la],matrix.matrix[la][j],matrix.matrix[j][la]};
//                for (int k=0; k<4; k++){
//                    if (values[k]==Float.POSITIVE_INFINITY){
//                        values[k] = 16;
//                    }
//                }
//
//                if ((values[0]-values[1])-(values[2]-values[3])!= 0){
//                    return (int)(-(values[0]-values[1])+(values[2]-values[3]));
//                }else{
//                    return (int)(values[1]-values[3]);
//                }
//            }
//        });
//
//        for (int i=0; i<20; i++){
//            System.out.println(sortedIds.get(i)+": "+matrix.matrix[la][matrix.idsInOrder.indexOf(sortedIds.get(i))]+" "+matrix.matrix[matrix.idsInOrder.indexOf(sortedIds.get(i))][la]);
//        }

        List<List<Integer>> manhattan = mv.generateGraphNeighborList(matrix,38733,Float.POSITIVE_INFINITY);
        List<List<Integer>> jfk = mv.generateGraphNeighborList(matrix,8685,Float.POSITIVE_INFINITY);

        manhattan.remove(manhattan.size()-1);
        jfk.remove(jfk.size()-1);

        String base = "";//http://maps.googleapis.com/maps/api/staticmap?center=UpperManhattan,NY&zoom=10&size=400x524";
        mv.generateMapUrl(manhattan,11,base);
        mv.generateMapUrl(jfk, 11,base);
    }

    public List<List<Integer>> generateGraphNeighborList(final MatrixGenerator.MatrixWithHeader graph, final int conceptId, float inclusiveMax){
        List<Integer> neighbors = new ArrayList<Integer>();
        neighbors.addAll(geometries.keySet());
        final int conceptIndex = graph.idToIndex.get(conceptId);
        Collections.sort(neighbors, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                float d1 = graph.matrix[conceptIndex][graph.idToIndex.get(o1)];
                float d2 = graph.matrix[conceptIndex][graph.idToIndex.get(o2)];
                if (d1 == Float.POSITIVE_INFINITY)
                    d1 = 16;
                if (d2 == Float.POSITIVE_INFINITY)
                    d2 = 16;
                return (int) (d1 - d2);
            }
        });

        List<List<Integer>> list = new ArrayList<List<Integer>>();
        List<Integer> curSet = new ArrayList<Integer>();
        list.add(curSet);
        float curLevel = graph.matrix[conceptIndex][graph.idToIndex.get(neighbors.get(0))];
        for (int i=0; i<neighbors.size(); i++){
            float newLevel = graph.matrix[conceptIndex][graph.idToIndex.get(neighbors.get(i))];
            if (newLevel==curLevel){
                curSet.add(neighbors.get(i));
            }else if (newLevel<=inclusiveMax){
                curSet = new ArrayList<Integer>();
                curSet.add(neighbors.get(i));
                list.add(curSet);
                curLevel = newLevel;
            }else{
                break;
            }
        }
        return list;
    }

    public void generateMapUrl (List<List<Integer>> levels, int zoom, String base){
//        String[] colors = {"color:purple|","color:blue|","color:green|","color:yellow|","color:orange|",""};
//        String[] colors = {"","color:orange|","color:yellow|","color:green|","color:blue|","color:purple|","color:black|","color:brown|","color:gray|","color:white|"};
//        String[] colors = {"color:0x00FF00|","color:0x00EF00|","color:0x00DF00|","color:0x00CF00|","color:0x00BF00|","color:0x00AF00|","color:0x09F00|","color:0x008F00|","color:0x007F00|","color:0x006F00|","color:0x005F00|","color:0x004F00|","color:0x003F00|","color:0x002F00|","color:0x001F00|","color:0x000F00|","color:0x000000|",};
        List<String> colors1 = new ArrayList<String>();
        String[] dig = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        int inc = 3;
        colors1.add("color:0x000000|");
        for (int i=6; i<dig.length; i+=inc){
            colors1.add("color:0x00"+dig[i]+"000|");
        }
        for (int i=1; i<dig.length; i+=inc){
            colors1.add("color:0x"+dig[i]+"0FF"+dig[i]+"0|");
        }
        colors1.add("color:0xFFFFFF|");
        colors1.add("");
        String[] colors=new String[colors1.size()];
        colors1.toArray(colors);
        generateMapUrl ( levels,  zoom, colors, base);
    }

    public void generateMapUrl (List<List<Integer>> levels, int zoom, String[] colors, String base){
        String size = "size:mid|";

        // if unspecified, use first point as default
        if (base == null || base.length()==0){
            base = "http://maps.googleapis.com/maps/api/staticmap?center=";
            String base2 = "&zoom="+zoom+"&size=800x524";//&markers="+size+srcColor;
            Point source = (Point) geometries.get(levels.get(0).get(0));
            String sourceStr = String.format("%.4f,%.4f",source.getY(),source.getX());
            base += sourceStr + base2;// + sourceStr;
        }
        System.out.println(base);

        // generate url
        String current = base +"|";
        for(int i=0; i<levels.size(); i++){
            System.out.println("Starting level "+i);
            if (current.length()>1500){
                System.out.println("Length: "+(current.length()-1));
                System.out.println(current);
                current = base + "|";
            }
            current = current.substring(0,current.length()-1)+"&markers="+size+colors[i%colors.length];
            for (int j=0; j<levels.get(i).size();j++){
                Point p2 = (Point) geometries.get(levels.get(i).get(j));
                String p2Str = String.format("%.4f,%.4f|",p2.getY(),p2.getX());
                current += p2Str;
            }
        }

        System.out.println("Length: "+(current.length()-1));
        System.out.println(current.substring(0,current.length()-1));
    }
}
