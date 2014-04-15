package org.wikapidia.spatial.loader;

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
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.spatial.core.dao.postgis.PostGISDB;
import org.wikapidia.wikidata.WikidataDao;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 12/29/13.
 */
public class SpatialDataLoader {

    private static final Logger LOG = Logger.getLogger(SpatialDataLoader.class.getName());

    private final SpatialDataDao spatialDataDao;
    private final File spatialDataFolder;
    private final WikidataDao wdDao;
    private final PhraseAnalyzer analyzer;

    public SpatialDataLoader(SpatialDataDao spatialDataDao, WikidataDao wdDao, PhraseAnalyzer analyzer, File spatialDataFolder) {
        this.spatialDataDao = spatialDataDao;
        this.spatialDataFolder = spatialDataFolder;
        this.wdDao = wdDao;
        this.analyzer = analyzer;
    }

    //TODO: this should probably be adapted to the PipelineLoader structure
    private void loadExogenousData() throws WikapidiaException{


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
            throw new WikapidiaException(e);
        }


            // do wikidata
//            parseWikidataSpatialData();

    }



    private List<LayerStruct> getLayerStructs(){

        List<LayerStruct> layerStructs = Lists.newArrayList();

        for (File curRsFolder : spatialDataFolder.listFiles()){

            String curRsName = curRsFolder.getName();
            if (!curRsFolder.isHidden() && !curRsFolder.getName().startsWith("_disabled")){
                LOG.log(Level.INFO, "Found reference system: " + curRsName);
                for (File curFile : curRsFolder.listFiles()){
                    LayerStruct lStruct = null;
                    if (curFile.getName().endsWith(".shp")){
                        lStruct = new LayerStruct(curRsName, FileType.SHP, curFile);
                    }else if (curFile.getName().endsWith(".wkt")){
                        lStruct = new LayerStruct(curRsName, FileType.WKT, curFile);
                    }else if (curFile.isDirectory() && curFile.getName().endsWith("_shpgrp")){
                        lStruct = new LayerStruct(curRsName, FileType.SHPGRP, curFile);
                    }
                    if (lStruct != null) layerStructs.add(lStruct);
                }
            }
        }
        return layerStructs;
    }

    /**
     * Will attempt to spatiotag left ot right with attributes. Need to write up this documentation.
     */

    private void parseShapefile(LayerStruct struct) throws WikapidiaException{

        ShapefileReader shpReader;
        DbaseFileReader dbfReader;
        Geometry curGeometry;
        ShpFiles shpFile;

        LOG.log(Level.INFO,"Parsing data from file: " + struct.getDataFile().getName());

        try {

            shpFile = new ShpFiles(struct.getDataFile().getAbsolutePath());

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

                int i = 0;

                boolean found = false;
                while(i < numDbfFields && !found){
                    IDAttributeHandler attrHandler = attrHandlers.get(i);
                    Integer itemId = attrHandler.getWikidataItemIdForId(dbfReader.readField(i));
                    if (itemId != null){
                        spatialDataDao.saveGeometry(itemId, struct.getLayerName(), struct.getRefSysName(), curGeometry);
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
            throw new WikapidiaException("There was an error accessing an external geometry layer", e);
        } catch (IOException e) {
            throw new WikapidiaException("There was an error accessing an external geometry layer", e);
        } catch (DaoException e){
            throw new WikapidiaException(e);
        }

    }

    private void loadWikidataData() throws WikapidiaException{


        try {

            spatialDataDao.beginSaveGeometries();

            // this should eventually be moved into a config file or parameters of the parse
            List<WikidataLayerLoader> layerLoaders = Lists.newArrayList();
            layerLoaders.add(new EarthBasicCoordinatesWikidataLayerLoader(wdDao, spatialDataDao));
//        layerLoaders.add(new EarthInstanceOfCoordinatesLayerLoader(wdDao, spatialDataDao));

            for (WikidataLayerLoader layerLoader : layerLoaders) {
                LOG.log(Level.INFO, "Loading Wikidata layer(s): " + layerLoader.getClass().getName());
                layerLoader.loadData();
            }

            spatialDataDao.endSaveGeometries();

        }catch(DaoException e){
            throw new WikapidiaException(e);
        }

    }


//    /**
//     * These are a bunch of shapefiles of the format below aggregated into a single layer.
//     * The folder containing the shapefiles must end with "_shpgrp" for it to be recognized.
//     * @param struct
//     * @throws WikapidiaException
//     */
//    private void parseShapefileGroup(LayerStruct struct) throws WikapidiaException{
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
//            throw new WikapidiaException(e);
//        }
//
//    }


//    /**
//     * Handles custom WKT-based format. This is how the time reference system layers are stored.
//     */
//    private void parseWktFile(Integer startGeomId, LayerStruct lStruct) throws WikapidiaException{
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
//            throw new WikapidiaException(e);
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

    private static String TEMP_SPATIAL_DATA_FOLDER = "/Users/toby/Dropbox/spatial_data";

    public static void main(String args[]) throws ConfigurationException, WikapidiaException {

        //public PostGISDB(String host, Integer port, String schema, String db, String user, String pwd,
       // int maxConnections) throws DaoException{
//        try {
//            PostGISDB db = new PostGISDB("localhost",5432,"public","wikapidia_new","bjhecht","",15);
//        } catch (DaoException e) {
//            e.printStackTrace();
//        }

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("spatial-data-folder")
                        .withDescription("the folder with spatial data")
                        .create("f"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("phrase-analyzer")
                        .withDescription("the PhraseAnalyzer to use to map toponyms to Wikipedia pages (defaults to 'titleandredirect'). Will always choose the first candidate, no matter the minimum probability, so declarative PhraseAnalyzers should be used here.")
                        .create("p"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();

        String phraseAnalyzerName = cmd.getOptionValue("p","titleredirect"); // add to docs that this has to be
        PhraseAnalyzer phraseAnalyzer = conf.get(PhraseAnalyzer.class, phraseAnalyzerName);

        //String spatialDataFolderPath = cmd.getOptionValue('f');
        File spatialDataFolder = new File("/Users/toby/Dropbox/spatial_data_temp");
        String spatialDataFolderPath = new String("/Users/toby/Dropbox/spatial_data_temp");
        //File spatialDataFolder = new File(spatialDataFolderPath); //TODO: fixme

        WikidataDao wdDao = conf.get(WikidataDao.class);
        SpatialDataDao spatialDataDao = conf.get(SpatialDataDao.class);

        LOG.log(Level.INFO, "Preparing to spatiotag data in folder: '" + spatialDataFolderPath + "'");


        //(SpatialDataDao spatialDataDao, WikidataDao wdDao, PhraseAnalyzer analyzer, File spatialDataFolder)
        SpatialDataLoader loader = new SpatialDataLoader(spatialDataDao, wdDao, phraseAnalyzer, spatialDataFolder);
        loader.loadWikidataData();
        //loader.loadExogenousData();



    }

    private static Collection<String> getRsNameCol (File folder, String refSysList) throws WikapidiaException{

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
            throw new WikapidiaException("Illegal reference system names found: " + StringUtils.join(validRsNames));
        }

        return rVal;

    }

}
