package org.wikibrain.spatial.loader;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.postgis.PostGISDB;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.wikidata.WikidataDao;
import sun.net.www.content.text.Generic;

import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 12/29/13.
 */
public class SpatialDataLoader {

    private static final Logger LOG = Logger.getLogger(SpatialDataLoader.class.getName());

    private final MetaInfoDao metaDao;
    private final SpatialDataDao spatialDataDao;
    private final SpatialDataFolder spatialDataFolder;
    private final WikidataDao wdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final PhraseAnalyzer analyzer;
    private final LanguageSet langs;

    public SpatialDataLoader(MetaInfoDao metaDao, SpatialDataDao spatialDataDao, WikidataDao wdDao, PhraseAnalyzer analyzer, SpatialDataFolder spatialDataFolder, LanguageSet langs) {
        this.metaDao = metaDao;
        this.spatialDataDao = spatialDataDao;
        this.spatialDataFolder = spatialDataFolder;
        this.wdDao = wdDao;
        this.analyzer = analyzer;
        this.langs = langs;
        this.lpDao = lpDao;
        this.upDao = upDao;
    }

    //TODO: this should probably be adapted to the PipelineLoader structure
    private void loadExogenousData() throws WikiBrainException{


        try {

            // *** DO EXOGENOUS SHAPEFILES ***

            spatialDataDao.beginSaveGeometries();
            List<LayerStruct> layerStructs = getLayerStructs();
            for (LayerStruct layerStruct : layerStructs) {

                if (layerStruct.fileType.equals(FileType.SHP)) {

                    parseShapefile(layerStruct);

                }
            }

            spatialDataDao.endSaveGeometries();


        }catch(DaoException e){
            throw new WikiBrainException(e);
        }


            // do wikidata
//            parseWikidataSpatialData();

    }



    private List<LayerStruct> getLayerStructs(){

        List<LayerStruct> layerStructs = Lists.newArrayList();

        for (String refSysName : spatialDataFolder.getReferenceSystemNames()){

            LOG.log(Level.INFO, "Found reference system: " + refSysName);
            for (File curFile : spatialDataFolder.getRefSysFolder(refSysName).listFiles()){
                LayerStruct lStruct = null;
                if (curFile.getName().endsWith(".shp")){
                    lStruct = new LayerStruct(refSysName, FileType.SHP, curFile);
                }else if (curFile.getName().endsWith(".wkt")){
                    lStruct = new LayerStruct(refSysName, FileType.WKT, curFile);
                }else if (curFile.isDirectory() && curFile.getName().endsWith("_shpgrp")){
                    lStruct = new LayerStruct(refSysName, FileType.SHPGRP, curFile);
                }
                if (lStruct != null) layerStructs.add(lStruct);
            }

        }
        return layerStructs;
    }

    /**
     * Will attempt to spatiotag left ot right with attributes. Need to write up this documentation.
     */

    private void parseShapefile(LayerStruct struct) throws WikiBrainException{

        ShapefileReader shpReader;
        DbaseFileReader dbfReader;
        Geometry curGeometry;
        ShpFiles shpFile;

        LOG.log(Level.INFO,"Parsing data from file: " + struct.getDataFile().getName());

        try {

            shpFile = new ShpFiles(struct.getDataFile().getAbsolutePath());

            // 4326 is the World Geodetic Sysetm http://en.wikipedia.org/wiki/World_Geodetic_System
            shpReader = new ShapefileReader(shpFile, true, true, new GeometryFactory(new PrecisionModel(), 4326));
            dbfReader = new DbaseFileReader(shpFile, false, Charset.forName("UTF-8"));

            int numDbfFields = dbfReader.getHeader().getNumFields();

            List<IDAttributeHandler> attrHandlers = Lists.newArrayList();
            for (int i = 0; i < numDbfFields; i++){
                attrHandlers.add(IDAttributeHandler.getHandlerByFieldName(dbfReader.getHeader().getFieldName(i), wdDao, analyzer));
            }

            int foundGeomCount = 0;
            int missedGeomCount = 0;

            while(shpReader.hasNext()){

                curGeometry = (Geometry)shpReader.nextRecord().shape();
                dbfReader.read();

                boolean found = false;

                for (int i = 0; i < numDbfFields && !found; i++) {
                    IDAttributeHandler attrHandler = attrHandlers.get(i);
                    Integer itemId;
                    try {
                            itemId = ((IDAttributeHandler.TitleAttributeHandler) attrHandler).getWikidataItemIdForId(dbfReader.readField(i));
                    } catch (NullPointerException e){
                        if (i!= 1)
                            e.printStackTrace();
                        continue;
                    } catch (Exception e){
                        System.out.println("exception which is not nullpointer");
                        e.printStackTrace();
                        continue;
                    }
                    if (itemId != null && spatialDataDao.getGeometry(itemId, struct.getLayerName(), struct.getRefSysName()) == null){
                        spatialDataDao.saveGeometry(itemId, struct.getLayerName(), struct.getRefSysName(), curGeometry);
                        metaDao.incrementRecords(Geometry.class);
                        found = true;
                        foundGeomCount++;
                        if (foundGeomCount % 10 == 0){
                            LOG.log(Level.INFO, "Matched " + foundGeomCount + " geometries in layer '" + struct.getLayerName() + "' (" + struct.getRefSysName() + ")");
                        }
                    }
                    i++;
                }

                if (!found) missedGeomCount++;

            }

            double matchRate = ((double)foundGeomCount)/(foundGeomCount + missedGeomCount);
            LOG.log(Level.INFO, "Finished layer '" + struct.getLayerName() + "': Match rate = " + matchRate);
 
            dbfReader.close();
            shpReader.close();

        } catch (ShapefileException e) {
            throw new WikiBrainException("There was an error accessing an external geometry layer", e);
        } catch (IOException e) {
            throw new WikiBrainException("There was an error accessing an external geometry layer", e);
        } catch (DaoException e){
            throw new WikiBrainException(e);
        }

    }

    private void loadWikidataData() throws WikiBrainException{


        try {

            spatialDataDao.beginSaveGeometries();

            // this should eventually be moved into a config file or parameters of the parse
            List<WikidataLayerLoader> layerLoaders = Lists.newArrayList();
            layerLoaders.add(new EarthBasicCoordinatesWikidataLayerLoader(metaDao, wdDao, spatialDataDao));
//            layerLoaders.add(new EarthInstanceOfCoordinatesLayerLoader(wdDao, spatialDataDao));

            for (WikidataLayerLoader layerLoader : layerLoaders) {
                LOG.log(Level.INFO, "Loading Wikidata layer(s): " + layerLoader.getClass().getName());
                layerLoader.loadData(langs);
            }

            spatialDataDao.endSaveGeometries();

        }catch(DaoException e){
            throw new WikiBrainException(e);
        }

    }


//    /**
//     * These are a bunch of shapefiles of the format below aggregated into a single layer.
//     * The folder containing the shapefiles must end with "_shpgrp" for it to be recognized.
//     * @param struct
//     * @throws WikiBrainException
//     */
//    private void parseShapefileGroup(LayerStruct struct) throws WikiBrainException{
//
//        try{
//            String layerName = struct.getLayerName();
//            layerName = layerName.replaceAll("_shpgrp", "");
//            for (File f : struct.dataFile.listFiles()){
//                if (f.getName().endsWith(".shp")){
//                    Integer startGeomId = spatialDataDao.getMaximumGeomId() + 1;
//                    parseShapefile(startGeomId, struct);
//                }
//            }
//        }catch(DaoException e){
//            throw new WikiBrainException(e);
//        }
//
//    }


//    /**
//     * Handles custom WKT-based format. This is how the time reference system layers are stored.
//     */
//    private void parseWktFile(Integer startGeomId, LayerStruct lStruct) throws WikiBrainException{
//
//        try{
//
//            BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(lStruct.dataFile.getAbsolutePath()),
//                    "UTF-8"));
//            WKTReader wktReader = new WKTReader();
//            String curLine;
//
//            Integer geomId = startGeomId;
//
//            while ((curLine = fileReader.readLine()) != null){
//
//                String[] parts = curLine.split("\t");
//
//                String langCode = parts[0];
//                Language lang = Language.getByLangCode(langCode);
//                String name = parts[1];
//                LocalPage lp = getLocalPageFromToponymUsingPhraseAnalyzer(name, lang);
//                if (lp == null) continue;
//
//                String wkt = parts[2];
//                Geometry g = wktReader.read(wkt);
//
//                spatialDataDao.saveGeometry(geomId, lStruct.getLayerName(), lStruct.getRefSysName(), g);
//                spatioTagDao.saveSpatioTag(new SpatioTagDao.SpatioTagStruct(lp.toLocalId(), geomId));
//
//                geomId++;
//            }
//
//            fileReader.close();
//
//
//        }catch(Exception e){
//            throw new WikiBrainException(e);
//        }
//
//    }



    private static enum FileType{SHP, WKT, SHPGRP};
    private static class LayerStruct{

        private final String refSysName;
        private final File dataFile;
        private final FileType fileType;
        private final String layerName;

        public LayerStruct(String refSysName, FileType fileType, File dataFile) {
            this.refSysName = refSysName;
            this.dataFile = dataFile;
            this.fileType = fileType;
            this.layerName = FilenameUtils.removeExtension(dataFile.getName());
        }

        public String getLayerName(){
            return layerName;
        }

        public String getRefSysName(){
            return refSysName;
        }

        public File getDataFile(){
            return dataFile;
        }

    }

    private static String TEMP_SPATIAL_DATA_FOLDER = "/Users/toby/Dropbox/spatial_data_wikibrain";

    public static void main(String args[]) {

        try {

            Options options = new Options();
            options.addOption("f", true, "The spatial data folder, structured as defined in the documentation.");
            options.addOption("p", true, "The PhraseAnalyzer to use to map toponyms to Wikipedia pages (defaults to 'titleandredirect'). Will always choose the first candidate, no matter the minimum probability, so declarative PhraseAnalyzers should be used here.");
            options.addOption("s", true, "Can be one more more of 'wikidata','exogenous', 'pagehitlist' (comma delimit). If nothing is entered, does all steps.");

            EnvBuilder.addStandardOptions(options);

            CommandLineParser parser = new PosixParser();
            CommandLine cmd;

            cmd = parser.parse(options, args);

            Env env = new EnvBuilder(cmd).build();
            Configurator conf = env.getConfigurator();

            String phraseAnalyzerName = cmd.getOptionValue("p", "titleredirect"); // add to docs that this has to be
            PhraseAnalyzer phraseAnalyzer = conf.get(PhraseAnalyzer.class, phraseAnalyzerName);
            MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

            String spatialDataFolderPath = cmd.getOptionValue("f", null);
            File spatialDataFolderFile;
            if (spatialDataFolderPath == null){
                spatialDataFolderFile = new File("spatial_data");
            }else{
                spatialDataFolderFile = new File(spatialDataFolderPath);
            }
            SpatialDataFolder spatialDataFolder = new SpatialDataFolder(spatialDataFolderFile);

            WikidataDao wdDao = conf.get(WikidataDao.class);
            SpatialDataDao spatialDataDao = conf.get(SpatialDataDao.class);
            LocalPageDao localPageDao = conf.get(LocalPageDao.class);
            UniversalPageDao universalPageDao = conf.get(UniversalPageDao.class);
            SpatialDataLoader loader = new SpatialDataLoader(spatialDataDao, wdDao, phraseAnalyzer, spatialDataFolder, env.getLanguages(),localPageDao, universalPageDao );

//            String stepsValue = cmd.getOptionValue("s", "wikidata,gadm,download,exogenous"); // GADM temporarily disabled while we do new mappings

            String stepsValue = cmd.getOptionValue("s", "wikidata,exogenous");
            String[] steps = stepsValue.split(",");
            for (String step : steps) {
                String canonicalStep = step.trim().toLowerCase();
                if (canonicalStep.equals("wikidata")) {
                    LOG.log(Level.INFO, "Beginning to extract geographic information from Wikidata");
                    loader.loadWikidataData();
                }else if (canonicalStep.equals("gadm")){
                    LOG.log(Level.INFO, "Beginning to download and process GADM data (will be imported in exogenous step)");
                    new GADMConverter().downloadAndConvert(spatialDataFolder);
                    //spatialDataFolder.deleteSpecificFile("read_me.pdf", RefSys.EARTH); // TODO: Aaron, please move the following two lines into the correct place in GADMConverter
                    //spatialDataFolder.deleteLayer("gadm2", RefSys.EARTH);
                } else if (canonicalStep.equals("exogenous")) {
                    LOG.log(Level.INFO, "Beginning to load exogenous data");
                    loader.loadExogenousData();
                } else if (canonicalStep.equals("download")) {
                    LOG.log(Level.INFO, "Downloading shapefiles and moving them into place");
                    WikiBrainSpatialUtils.downloadZippedShapefileToReferenceSystem("http://www-users.cs.umn.edu/~bhecht/wikibrain/spatial_data/elements.zip",
                            RefSys.PERIODIC_TABLE, spatialDataFolder); // TODO: this should be moved to the config file at some point
                } else if (canonicalStep.equals("pagehitlist")){
                    LOG.log(Level.INFO, "Loading spatial entities from PageHitList.txt");
                    loader.loadSignificantGeometries(new File("PageHitList.txt"));
                } else {
                    throw new Exception("Illegal step: '" + step + "'");
                }
            }


        //(SpatialDataDao spatialDataDao, WikidataDao wdDao, PhraseAnalyzer analyzer, File spatialDataFolder)
        //loader.loadWikidataData();
        //loader.loadExogenousData();

            LOG.info("optimizing database.");
            conf.get(WpDataSource.class).optimize();


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadSignificantGeometries(File file){
        List<Integer> pageHitList = new ArrayList<Integer>();
        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNext()){
                int nextInt = scanner.nextInt();
                pageHitList.add(nextInt);
            }
        } catch(IOException e){
            System.out.println("cannot find significant geometries input");
        }
        try {
            spatialDataDao.beginSaveGeometries();
            Map<Integer, Geometry> geometries = spatialDataDao.getAllGeometriesInLayer("wikidata", "earth");
            int count = 0;
            for (Integer i: pageHitList) {
                if (geometries.get(i)!= null){
//                    System.out.println("processing id "+i);
//                    System.out.println(geometries.get(i).toString());
                    spatialDataDao.saveGeometry(i.intValue(), "significant", "earth", geometries.get(i));
                    count++;
//                    System.out.println("added geometry with id "+i);
                } else {
                    System.out.println(i);
                }
            }
            spatialDataDao.endSaveGeometries();
            System.out.println();
            System.out.println(count);
        } catch(DaoException e){
            System.out.println("Dao exception");
            e.printStackTrace();
        }
    }

    private static Collection<String> getRsNameCol (File folder, String refSysList) throws WikiBrainException{

        boolean useAll = refSysList.equals("all");

        Set<String> validRsNames = Sets.newHashSet();
        if (!useAll){
            String[] refSysListArr = refSysList.split(",");
            for(String t : refSysListArr){
                validRsNames.add(t.trim());
            }
        }

        List<String> rVal = new ArrayList<String>();

        for(File f : folder.listFiles()){
            if (f.isDirectory()){
                if (useAll || validRsNames.contains(f.getName())){
                    rVal.add(f.getName());
                    validRsNames.remove(f.getName());
                }
            }
        }

        if (!useAll && validRsNames.size() > 0){
            throw new WikiBrainException("Illegal reference system names found: " + StringUtils.join(validRsNames));
        }

        return rVal;

    }

}
