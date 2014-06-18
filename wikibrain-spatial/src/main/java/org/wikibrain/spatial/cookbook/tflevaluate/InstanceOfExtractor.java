package org.wikibrain.spatial.cookbook.tflevaluate;

import com.sun.org.apache.bcel.internal.generic.LAND;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.Dao;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
//import org.wikibrain.core.jooq.tables.WikidataEntityLabels;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InstanceOfExtractor {

    private static int WIKIDATA_CONCEPTS = 1;
    public static int COUNTRY = 0 , STATE = 1 , CITY = 2 , NATURAL = 3 , WEIRD = 4 , LANDMARK = 5;
    public static int MAX = 6;
    private Set<String>[] scaleKeywords = new Set[MAX];
    private Set<Integer>[] scaleIds = new Set[MAX];
    private String[] fileNames = {"country.txt", "state.txt", "city.txt", "natural.txt", "weird.txt", "landmark.txt"};
    private SpatialDataDao sdDao;
    private UniversalPageDao upDao ;
    private LocalPageDao lDao ;
    private WikidataDao wdao ;
    private static final boolean DEBUG = true;
    private static final Language CUR_LANG = Language.EN;

    private static final Logger LOG = Logger.getLogger(InstanceOfExtractor.class.getName());


    public InstanceOfExtractor(Env env ) throws ConfigurationException{
        Configurator c = env.getConfigurator();
        sdDao = c.get(SpatialDataDao.class);
        upDao = c.get(UniversalPageDao.class);
        lDao = c.get(LocalPageDao.class);
        wdao = c.get(WikidataDao.class);
    }

    public InstanceOfExtractor(SpatialDataDao sDao, UniversalPageDao uDao, LocalPageDao lDao, WikidataDao wDao) {
        sdDao = sDao;
        upDao = uDao;
        this.lDao = lDao;
        wdao = wDao;
    }


    public static void main (String[] args) {
        Env env = null;
        InstanceOfExtractor ioe = null;
        try{
            env = EnvBuilder.envFromArgs(args);
            ioe = new InstanceOfExtractor(env);
//            ioe.loadScaleKeywords();
//            ioe.loadScaleIds(new File("scaleIds.txt"));
//            ioe.generateScaleId();
            // print out concepts in relevant category
//            ioe.printScale(CITY);
//            ioe.generateRecallTest(150);
//            ioe.printScaleId(CITY);
            Set<String> set = ioe.extractInstanceOfList();
            int count = 0;
            for (String s: set){
                System.out.println(s);
                count++;
                if (count%10000==0){
                    System.out.println("============================ "+count+" ================================");
                }
            }

        }catch(DaoException e){
            System.out.println(e);
        }catch(ConfigurationException e){
            System.out.println(e);
        }

    }

    /**
     * Creates a set of titles of all spatial concepts in Wikipedia
     * (that are associated with the language downloaded, known as CUR_LANG)
     *
     * @throws DaoException
     */
    public Set<String> extractInstanceOfList() throws DaoException {

        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        // set of ids that belong to spatial descriptors ("instance of" labels)
        HashSet<Integer> set = new HashSet<Integer>();
        // set of titles of those pages
        HashSet<String> titleSet = new HashSet<String>();
        // used to print intermediate counts when DEBUG is true
        int count = 0;

        // loop over known concept geometries
        for (Integer conceptId : geometries.keySet()){

            // use getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();

            // print intermediate counts when DEBUG is true
            count++;
            if (DEBUG&&count%10000==0){
                LOG.log(Level.INFO,"++++++++++++++++++++++++++++++++++ "+count+" ++++++++++++++++++++++++++++++++");
            }

            // loop over Wikidata statements associated with a concept
            for (WikidataStatement st: list){

                // if property is not null and the property id corresponds to "instance of"
                if (st.getProperty() != null && st.getProperty().getId()==31 ){

                    // get id of "instance of" label
                    int id=0;
                    try {
                        id = st.getValue().getIntValue();
                    }catch (NullPointerException e){
                        System.out.println("Null pointer exception for "+conceptId);
                        continue;
                    }

                    // add that id to our set of "instance of" label ids
                    set.add(id);

                    //some concept local pages are missing
                    try {
                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        LocalPage page = lDao.getById(CUR_LANG, concept2.getLocalId(CUR_LANG));
                        titleSet.add(page.getTitle().toString());

                    } catch(Exception e){
                        // this sometimes works to get a label title even when there is not an associated local page
                        if (wdao.getItem(id).getLabels().get(Language.EN)!= null){
                            titleSet.add(wdao.getItem(id).getLabels().get(Language.EN).toString());
                         } else if (wdao.getItem(id).getLabels().get(Language.SIMPLE)!= null){
                            titleSet.add(wdao.getItem(id).getLabels().get(Language.SIMPLE).toString());
                        }
                    }
                }
            }
        }
        return titleSet;
    }

    /**
     * This loads in scale keywords so that other methods can
     * use them to decide in which scale to classify a spatial concept.
     * The scale keywords are in plain text files, one keyword per line.
     * The filenames are pre-defined.
     *
     * @throws FileNotFoundException
     */
    public void loadScaleKeywords() throws FileNotFoundException{
        for (int i = 0; i<MAX; i++) {
            scaleKeywords[i] = new HashSet<String>();
            Scanner scanner = new Scanner(new File(fileNames[i]));
            while (scanner.hasNextLine()) {
                scaleKeywords[i].add(scanner.nextLine());
            }
        }
    }

    /**
     * This loads an array of sets where each element in the arrray
     * is a set of concept ids associated with a given scale.
     *
     * @param file The file this array of sets is stored in.
     */
    public void loadScaleIds(File file){

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            scaleIds = (Set[]) ois.readObject();
            ois.close();

        } catch (Exception e) {
            System.out.println("file not found");
        }
    }

    /**
     *
     * @throws DaoException Could happen when trying to access pages or spatial information
     * @throws IOException Could happen when trying to write the generated ids to their file
     */
    public void generateScaleId() throws DaoException,IOException{

        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        // initiate scale sets
        for (int i=0; i<MAX; i++   ){
            scaleIds[i] = new HashSet<Integer>();
        }

        // counter print, for debug updates
        int count = 0;

        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(CUR_LANG,concept.getLocalId(CUR_LANG));

            // counter print
            if (DEBUG && count%10000 == 0){
                LOG.log(Level.INFO, "++++++++++++++++++++++++++++++++++ " + count + " ++++++++++++++++++++++++++++++++");
            }
            count++;

            // use getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();

            // break for loop after first proper "instance of" label found for this conceptId
            boolean found = false;

            // loop over this conceptId's statements
            for (WikidataStatement st: list){

                //simple English misses some concept local pages
                if (st.getProperty() != null && st.getProperty().getId()==31 ){
                    // the id belongs to the "instance of" label
                    int id = st.getValue().getIntValue();
                    try {
                        // universal id corresponding to "instance of" label
                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        // local page ditto
                        LocalPage page = lDao.getById(CUR_LANG, concept2.getLocalId(CUR_LANG));

                        // check if there's a spatial keyword in this instance-of label title
                        if (findMatch(conceptId, page.getTitle().toString())){
                            found = true;
                            break;
                        }

                    } catch(NullPointerException e){
                        // try to check title in alternate manner because sometimes can't find local page
                        if (wdao.getItem(id).getLabels().get(Language.EN)!= null){
                            if (findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.EN).toString())){
                                found = true;
                                break;
                            }
                        } else if (wdao.getItem(id).getLabels().get(Language.SIMPLE)!= null) {
                            if (findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.SIMPLE).toString())) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            // if didn't find a valid keyword in any of the instance-of descriptions of this concept,
            // do work-arounds
            if (!found) {
                // counties get special special treatment
                if (lpage.getTitle().toString().toLowerCase().contains("county,")){
                    scaleIds[CITY].add(conceptId);
                }
                // assume others with commas are cities
                else if (lpage.getTitle().toString().contains(",")) {
                    scaleIds[CITY].add(conceptId);
                    System.out.println(lpage.getTitle().toString());
                }
                // assume everything else is a landmark
                else {
                    scaleIds[LANDMARK].add(conceptId);
                }
            }
        }

        // write scale ids out to a file
        FileOutputStream fos = new FileOutputStream("scaleIds"+CUR_LANG+".txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(scaleIds);
        oos.close();
    }

    /**
     * Check if there is a match between the instance-of label and one of the scale keywords.
     * If yes, add it to the appropriate set.
     *
     * @param id The id of the concept
     * @param instanceOfLabel The label of an instance-of property associated with that concept
     * @return Whether or not the id was placed into a scale set
     */
    private boolean findMatch(int id, String instanceOfLabel){
        // pre-process the string
        instanceOfLabel = instanceOfLabel.toLowerCase();
        if (instanceOfLabel.endsWith(" (simple)")) {
            instanceOfLabel = instanceOfLabel.substring(0, instanceOfLabel.length() - 9);
        }
        if (instanceOfLabel.endsWith(",")) {
            instanceOfLabel = instanceOfLabel.substring(0, instanceOfLabel.length() - 1);
        }

        // check for a match
        for (int i=0; i<MAX; i++) {
            if (match(scaleKeywords[i], instanceOfLabel)) {
                scaleIds[i].add(id);
                return true;
            }
        }

        // if no match found, return false
        return false;
    }

    /**
     * Check if the pageTitle, or any of its constituent words (space-delimited) are in the given keyword set
     *
     * @param scaleKeyword
     * @param pageTitle
     * @return Whether or not a match was found with the given keyword set
     */
    private boolean match(Set<String> scaleKeyword, String pageTitle){
        // look for full string in the keyword set
        if (scaleKeyword.contains(pageTitle)) {
            return true;
        }
        // if not there, look for part strings
        else{
            String[] tokens = pageTitle.split(" ");
            for (String s: tokens){
                if (scaleKeyword.contains(s)) {
                    return true;
                }
            }
        }
        // if still nothing found, return false
        return false;
    }

    /**
     * Get the scale associated with a concept id, if any. If none found, returns -1
     *
     * @param id A concept id
     * @return The scale associated with it (from one of the final ints)
     */
    public int getScale(int id){
        for (int i=0; i<MAX; i++){
            if (scaleIds[i].contains(id)){
                return i;
            }
        }
        return -1;
    }

    /**
     * Prints names of all concepts in a given scale
     *
     * @param scaleId One of the final ints
     * @throws DaoException
     */
    public void printScale( int scaleId) throws DaoException{
        for (Integer conceptId : scaleIds[scaleId]){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(CUR_LANG,concept.getLocalId(CUR_LANG));
            System.out.println(lpage.getTitle().toString());
        }
        System.out.println(scaleIds[scaleId].size());
    }

    /**
     * Prints ids of all concepts in a given scale
     *
     * @param scaleId
     * @throws DaoException
     */
    public void printScaleId( int scaleId) throws DaoException{
        for (Integer conceptId : scaleIds[scaleId]){
            System.out.println(conceptId);
        }
    }

    /**
     * Provides the specified number of concept ids from the given scale set,
     * so that one can check what percentage of them have been placed correctly.
     *
     * @param scaleId
     * @param size
     * @throws Exception
     */
    public void generatePrecisionCalculation(int scaleId, int size) throws Exception {
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(scaleIds[scaleId]);
        Collections.shuffle(list);
        for (int i = 0; i<size; i++){
//            UniversalPage concept = upDao.getById(list.get(i), WIKIDATA_CONCEPTS);
//            LocalPage lpage = lDao.getById(CUR_LANG,concept.getLocalId(CUR_LANG));
            System.out.println(i + ". " + list.get(i));, lpage.getTitle().toString()
        }
    }

    /**
     * Get the set of concepts ids associated with a given scale.
     *
     * @param id
     * @return
     */
    public Set<Integer> getScaleIdSet(int id){
        return scaleIds[id];
    }

    /**
     * Provides the specified number of spatial concepts. To perform a recall test,
     * manually choose the first some number of them that are in the scale you are interested in,
     * and then see how many of these ids were placed in the correct scale set.
     *
     * @param size
     * @throws DaoException
     */
    public void generateRecallTest(int size) throws  DaoException {
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(geometries.keySet());
        Collections.shuffle(list);
        for (int i = 0; i< size; i++){
            System.out.println(i+". "+list.get(i));
        }
    }
}
