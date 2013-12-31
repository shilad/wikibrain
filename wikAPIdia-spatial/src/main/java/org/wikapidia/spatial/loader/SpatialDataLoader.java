package org.wikapidia.spatial.loader;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
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
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.spatial.core.dao.SpatioTagDao;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 12/29/13.
 */
public class SpatialDataLoader {

    private static final Logger LOG = Logger.getLogger(SpatialDataLoader.class.getName());

    private static int NUMBER_OF_NAME_FIELDS = 3;


    private final PhraseAnalyzer phraseAnalyzer;
    private final float phraseProbabilityThreshold;
    private final SpatialDataDao spatialDataDao;
    private final SpatioTagDao spatioTagDao;
    private final Collection<String> refSysNamesToLoad; // (if null, will load all in folder)
    private final File spatialDataFolder;

    public SpatialDataLoader(PhraseAnalyzer phraseAnalyzer, float phraseProbabilityThreshold, SpatialDataDao spatialDataDao,
                             SpatioTagDao spatioTagDao, Collection<String> refSysNamesToLoad, File spatialDataFolder) {
        this.phraseAnalyzer = phraseAnalyzer;
        this.phraseProbabilityThreshold = phraseProbabilityThreshold;
        this.spatialDataDao = spatialDataDao;
        this.spatioTagDao = spatioTagDao;
        this.refSysNamesToLoad = refSysNamesToLoad;
        this.spatialDataFolder = spatialDataFolder;
    }

    public void load() throws WikapidiaException {

        try{
            List<LayerStruct> layerStructs = getLayerStructs();
            for (LayerStruct layerStruct : layerStructs){
                Integer startGeomId = spatialDataDao.getMaximumGeomId() + 1;
                if (layerStruct.fileType.equals(FileType.SHP)){
                    parseShapefile(startGeomId, layerStruct);
                }
                if (layerStruct.fileType.equals(FileType.WKT)){
                    parseWktFile(startGeomId, layerStruct);
                }
                if (layerStruct.fileType.equals(FileType.SHPGRP)){
                    parseShapefileGroup(layerStruct);
                }
            }
        }catch(DaoException e){
            throw new WikapidiaException(e);
        }
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
     * The specs of these files must be as follows:
     * * First column can either hold a valid language code or the first times. If language code, it must be titled "lang". OBJECT_ID columns
     * 		should be deleted. If no language column is specified, the assumption is that names/aliases are in English.
     * * Up to NUMBER_OF_NAME_FIELDS additional columns (3 cols without language, 4 with) are dedicated to names and aliases of geometries
     * * See 'dbpedia.shp' for example of language dbf
     * * See 'admin.shp' for example of non-language dbf with several names/aliases.
     */
    private void parseShapefile(int firstAvailableGeomId, LayerStruct struct) throws WikapidiaException{

        ShapefileReader shpReader;
        DbaseFileReader dbfReader;
        Geometry curGeometry;
        ShpFiles shpFile;

        LOG.log(Level.INFO,"Parsing data from file: " + struct.getDataFile().getName());

        try {

            Integer geomId = firstAvailableGeomId;

            shpFile = new ShpFiles(struct.getDataFile().getAbsolutePath());

            shpReader = new ShapefileReader(shpFile, true, true, new GeometryFactory(new PrecisionModel(), 4326));
            dbfReader = new DbaseFileReader(shpFile, false, Charset.forName("UTF-8"));
            int counter = 0;

            DbaseFileHeader dbfHeader = dbfReader.getHeader();
            boolean langColumn = false;
            if (dbfHeader.getFieldName(0).equals("lang_code")){
                langColumn = true;
            }
            int startField = (langColumn) ? 1 : 0;
            int endField = Math.min(dbfReader.getHeader().getNumFields()-1, NUMBER_OF_NAME_FIELDS+startField-1); // +1 for langs

            while(shpReader.hasNext()){

                curGeometry = (Geometry)shpReader.nextRecord().shape();
                dbfReader.read();

                spatialDataDao.saveGeometry(geomId, struct.getLayerName(), struct.getRefSysName(), curGeometry);

                String langCode = (langColumn) ? (String)dbfReader.readField(0) : "en";
                Language lang = Language.getByLangCode(langCode);


                // try to match the entity to a Wikipedia page by trying the first NUMBER_OF_NAME_FIELDS fields
                for(int i = startField; i <= endField; i++){
                    Set<String> names = Sets.newHashSet(); // <-- to avoid duplicates
                    Object tempNameObj = dbfReader.readField(i);
                    if (tempNameObj instanceof String){
                        String tempName = (String)tempNameObj;
                        if (tempName.trim().length() != 0){
                            names.add(tempName);
                        }
                    }
                    for (String curName : names){
                        try{
                            curName = StringEscapeUtils.unescapeJava(curName);
                            LocalPage lp = getLocalPageFromToponymUsingPhraseAnalyzer(curName, lang);
                            if (lp != null){
                                spatioTagDao.saveSpatioTag(new SpatioTagDao.SpatioTagStruct(lp.toLocalId(),geomId));
                            }
                        }catch(IllegalArgumentException e){
                            LOG.log(Level.WARNING, e.getMessage()); // this rarely happens due to encoding issues, likely in GeoTool's shpfiles writer ("Less than 4 hex digits in unicode value: '\\u05' due to end of CharSequence");
                        }
                    }
                }

                counter++;
                if (counter % 1000 == 0){
                    LOG.log(Level.INFO, String.format("Done with %d spatial objects in %s (%s)", counter, struct.getLayerName(), struct.getDataFile().getName()));
                }

                geomId++;
            }
 
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

    private LocalPage getLocalPageFromToponymUsingPhraseAnalyzer(String toponym, Language lang) throws DaoException{
        LinkedHashMap<LocalPage, Float> candidate = phraseAnalyzer.resolveLocal(lang, toponym, 1);
        if (candidate.size() == 0) return null;
        LocalPage lp = candidate.keySet().iterator().next();
        if (candidate.get(lp) >= phraseProbabilityThreshold){
            return lp;
        }else{
            return null;
        }
    }

    /**
     * These are a bunch of shapefiles of the format below aggregated into a single layer.
     * The folder containing the shapefiles must end with "_shpgrp" for it to be recognized.
     * @param struct
     * @throws WikapidiaException
     */
    private void parseShapefileGroup(LayerStruct struct) throws WikapidiaException{

        try{
            String layerName = struct.getLayerName();
            layerName = layerName.replaceAll("_shpgrp", "");
            for (File f : struct.dataFile.listFiles()){
                if (f.getName().endsWith(".shp")){
                    Integer startGeomId = spatialDataDao.getMaximumGeomId() + 1;
                    parseShapefile(startGeomId, struct);
                }
            }
        }catch(DaoException e){
            throw new WikapidiaException(e);
        }

    }


    /**
     * Handles custom WKT-based format. This is how the time reference system layers are stored.
     */
    private void parseWktFile(Integer startGeomId, LayerStruct lStruct) throws WikapidiaException{

        try{

            BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(lStruct.dataFile.getAbsolutePath()),
                    "UTF-8"));
            WKTReader wktReader = new WKTReader();
            String curLine;

            Integer geomId = startGeomId;

            while ((curLine = fileReader.readLine()) != null){

                String[] parts = curLine.split("\t");

                String langCode = parts[0];
                Language lang = Language.getByLangCode(langCode);
                String name = parts[1];
                LocalPage lp = getLocalPageFromToponymUsingPhraseAnalyzer(name, lang);
                if (lp == null) continue;

                String wkt = parts[2];
                Geometry g = wktReader.read(wkt);

                spatialDataDao.saveGeometry(geomId, lStruct.getLayerName(), lStruct.getRefSysName(), g);
                spatioTagDao.saveSpatioTag(new SpatioTagDao.SpatioTagStruct(lp.toLocalId(), geomId));

                geomId++;
            }

            fileReader.close();


        }catch(Exception e){
            throw new WikapidiaException(e);
        }

    }



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

    private static String TEMP_SPATIAL_DATA_FOLDER = "/Users/bjhecht/Dropbox/spatial_data";

    public static void main(String args[]) throws ConfigurationException, WikapidiaException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("ref-sys")
                        .withDescription("the reference systems to load (separated by commas) (defaults to 'earth'), can put 'all' for all in spatial data folder")
                        .withValueSeparator(',')
                        .create("r"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("phrase-analyzer")
                        .withDescription("the PhraseAnalyzer to use to map toponyms to Wikipedia pages (defaults to 'titleandredirect')")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("prob-thresh")
                        .withDescription("the minimum probability for toponym to be matched to a Wikipedia page (defaults to 1.0)")
                        .create("t"));
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

        String refSysList = cmd.getOptionValue("r","earth");
        String phraseAnalyzerName = cmd.getOptionValue("p","titleandredirect");
        String propThreshStr = cmd.getOptionValue("t","1.0");

        File spatialDataFolder = new File(TEMP_SPATIAL_DATA_FOLDER);

        Collection<String> refSysNameCollection = getRsNameCol(spatialDataFolder,refSysList);
        PhraseAnalyzer phraseAnalyzer = conf.get(PhraseAnalyzer.class, phraseAnalyzerName);
        float propThresh = Float.parseFloat(propThreshStr);
        SpatialDataDao spatialDataDao = conf.get(SpatialDataDao.class);
        SpatioTagDao spatioTagDao = conf.get(SpatioTagDao.class);

        SpatialDataLoader loader = new SpatialDataLoader(phraseAnalyzer, propThresh, spatialDataDao, spatioTagDao,
                refSysNameCollection, spatialDataFolder);

        loader.load();

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
