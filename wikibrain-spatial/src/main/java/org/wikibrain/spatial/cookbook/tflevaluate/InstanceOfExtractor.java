package org.wikibrain.spatial.cookbook.tflevaluate;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
//import org.wikibrain.core.jooq.tables.WikidataEntityLabels;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
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


    // only need to change order here
    public static int WEIRD = 0, LANDMARK = 1, COUNTY = 2 ,COUNTRY = 3 , STATE = 4,  CITY =5 , NATURAL =6 ;
    private String[] fileNames = { "weird.txt", "landmark.txt", "county.txt",  "country.txt", "state.txt", "city.txt", "natural.txt" };
    public static int NUM_SCALES =7;
    private Set<String>[] scaleKeywords = new Set[NUM_SCALES];
    private Set<Integer>[] scaleIds = new Set[NUM_SCALES];

    private SpatialDataDao sdDao;
    private UniversalPageDao upDao;
    private LocalPageDao lDao;
    private WikidataDao wdao;
    private static final boolean DEBUG = true;
    private static final Language CUR_LANG = Language.SIMPLE;
    private Map<Integer,Set<Integer>> countryToStateMap;

    private static final Logger LOG = Logger.getLogger(InstanceOfExtractor.class.getName());


    public InstanceOfExtractor(Configurator c) throws ConfigurationException {
        this(c.get(SpatialDataDao.class), c.get(UniversalPageDao.class), c.get(LocalPageDao.class), c.get(WikidataDao.class));
    }

    public InstanceOfExtractor(SpatialDataDao sDao, UniversalPageDao uDao, LocalPageDao lDao, WikidataDao wDao) {
        sdDao = sDao;
        upDao = uDao;
        this.lDao = lDao;
        wdao = wDao;

        try {
            FileInputStream fis = new FileInputStream(new File("countryToStateMap.txt"));
            ObjectInputStream ois = new ObjectInputStream(fis);
            countryToStateMap = (Map) ois.readObject();
            ois.close();

        } catch (IOException e) {
            System.out.println("file not found");
        } catch (ClassNotFoundException e) {
            System.out.println("object in file was wrong class");
        }


//        try {
//            countryToStateMap = loadHierarchicalData();
//
//        }catch(DaoException e){
//            countryToStateMap = new HashMap<Integer, Set<Integer>>();
//            System.out.println("Could not load country/state gadm info");
//        }
    }


    public static void main (String[] args)  {


//        String pageTitle = "hello, h,h( hi))";
//        String[] tokens = pageTitle.split("([, ]){1,}");
//
//        for (int i=0; i<tokens.length; i++){
//            System.out.println("+"+tokens[i]+"+");
//        }

        Env env = null;
        InstanceOfExtractor ioe = null;
        try {
            env = EnvBuilder.envFromArgs(args);
            ioe = new InstanceOfExtractor(env.getConfigurator());

            // write scale ids out to a file
//            FileOutputStream fos = new FileOutputStream("countryToStateMap.txt");
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            oos.writeObject(ioe.countryToStateMap);
//            oos.close();

//            for (int country1:ioe.countryToStateMap.keySet()){
//                for (int country2:ioe.countryToStateMap.keySet()){
//                    if (country1!=country2){
//                        Set<Integer> set1 = ioe.countryToStateMap.get(country1);
//                        Set<Integer> set2 = ioe.countryToStateMap.get(country2);
//                        set1.retainAll(set2);
//                        if (set1.isEmpty()){
////                            System.out.println(country1+" "+country2+" "+set1);
//                        }
//                    }
//                }
//            }
//
//            System.out.println(ioe.countryToStateMap.get(30));
//            System.out.println(ioe.countryToStateMap.get(16));
            ioe.loadScaleKeywords();
//            ioe.loadScaleIds();
            ioe.generateScaleId();
            ioe.createScaleFile();
            // print out concepts in relevant category
//            ioe.printScale(STATE);

//            ioe.generateRecallTest(50);

//            ioe.printScaleId(COUNTRY);

//            Set<String> set = ioe.extractInstanceOfList();
//            int count = 0;
//            for (String s: set){
//                System.out.println(s);
//                count++;
//                if (count%10000==0){
//                    System.out.println("============================ "+count+" ================================");
//                }
//            }

        }catch(DaoException e){
            System.out.println(e);
        }catch(ConfigurationException e){
            System.out.println(e);
        }catch(IOException e){
            System.out.println(e);
        }

    }

    /**
     * Generate a map from countries(' conceptIds) to their states
     *
     * @return
     * @throws DaoException
     */
    public Map<Integer, Set<Integer>> loadHierarchicalData() throws DaoException {

        // Get all known country geometries
        Map<Integer, Geometry> countries = sdDao.getAllGeometriesInLayer("gadm0", "earth");
        LOG.log(Level.INFO, String.format("Found %d total countries, now loading countries", countries.size()));

        // Get all known state geometries
        Map<Integer, Geometry> states = sdDao.getAllGeometriesInLayer("gadm1", "earth");
        LOG.log(Level.INFO, String.format("Found %d total states, now loading states", states.size()));

        // Map of countries to states
        Map<Integer, Set<Integer>> countryStateMap = new HashMap<Integer, Set<Integer>>();

        // Loop over countries and states to generate this map
        for (int countryId: countries.keySet()){
            Geometry country = countries.get(countryId);
            countryStateMap.put(countryId,new HashSet<Integer>());
            for (int stateId: states.keySet()){
                try {
                    Geometry state = states.get(stateId);
                    if (country != null && state != null && country.contains(state.getInteriorPoint())) {
                        countryStateMap.get(countryId).add(stateId);
                    }
                }catch(TopologyException e){
                    //still add exception cases to countryStateMap
//                    countryStateMap.get(countryId).add(stateId);
                    System.out.println("Country "+countryId+" had topology exception with state "+stateId);
                }
            }
            System.out.println("Country " + countryId + " has " + countryStateMap.get(countryId).size() + " states: " + countryStateMap.get(countryId));
        }

        // return the map
        return countryStateMap;
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
        for (Integer conceptId : geometries.keySet()) {

            // use getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();

            // print intermediate counts when DEBUG is true
            count++;
            if (DEBUG && count % 10000 == 0) {
                LOG.log(Level.INFO, "++++++++++++++++++++++++++++++++++ " + count + " ++++++++++++++++++++++++++++++++");
            }

            // loop over Wikidata statements associated with a concept
            for (WikidataStatement st : list) {

                // if property is not null and the property id corresponds to "instance of"
                if (st.getProperty() != null && st.getProperty().getId() == 31) {

                    // get id of "instance of" label
                    int id = 0;
                    try {
                        id = st.getValue().getIntValue();
                    } catch (NullPointerException e) {
                        System.out.println("Null pointer exception for " + conceptId);
                        continue;
                    }

                    // add that id to our set of "instance of" label ids
                    set.add(id);

                    //some concept local pages are missing
                    try {
                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        LocalPage page = lDao.getById(CUR_LANG, concept2.getLocalId(CUR_LANG));
                        titleSet.add(page.getTitle().toString());

                    } catch (Exception e) {
                        // this sometimes works to get a label title even when there is not an associated local page
                        if (wdao.getItem(id).getLabels().get(Language.EN) != null) {
                            titleSet.add(wdao.getItem(id).getLabels().get(Language.EN).toString());
                        } else if (wdao.getItem(id).getLabels().get(Language.SIMPLE) != null) {
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
        for (int i = 0; i< NUM_SCALES; i++) {
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
     */
    public void loadScaleIds() {

        try {
            FileInputStream fis = new FileInputStream(new File("scaleIds"+CUR_LANG+".txt"));
            ObjectInputStream ois = new ObjectInputStream(fis);
            scaleIds = (Set[]) ois.readObject();
            ois.close();

        } catch (IOException e) {
            System.out.println("file not found");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("object in file was wrong class");
        }
    }

    public void writeScaleFile(String fullFileName) throws DaoException {

        File file = new File(fullFileName);
        BufferedWriter writer = null;
        {
            try {
                writer = new BufferedWriter(new FileWriter(file));
            } catch (Exception e) {
                System.out.println("Problem when trying to create new csv file");
            }
        }

        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        // counter print, for debug updates
        int count = 0;

        // loop over conceptIds
        for (Integer conceptId : geometries.keySet()) {

            // counter print, for debug updates
            if (count % 10000 == 0) {
                LOG.log(Level.INFO, "Have processed " + count + " spatial concepts.");
            }

            int scale = LANDMARK;
            for (int i = 0; i < NUM_SCALES; i++) {
                if (scaleIds[i].contains(conceptId)) {
                    scale = i;
                    break;
                }
            }
            // write this out to the file
            try {
                writer.write(conceptId + "\t" + scale + "\n");
            } catch (IOException e) {
                System.out.println("ioexception while writing concept " + conceptId + " to file with scale " + scale);
                e.printStackTrace();
            }

        }
    }

    /**
     * Separate concept ids by scale and save into a file.
     *
<<<<<<< HEAD
     * @throws DaoException Could happen when trying to access pages or spatial information
     * @throws IOException  Could happen when trying to write the generated ids to their file
=======
     * @throws org.wikibrain.core.dao.DaoException Could happen when trying to access pages or spatial information
     * @throws java.io.IOException Could happen when trying to write the generated ids to their file
>>>>>>> b491e3aa5121292efc6dc12cd7fcaf40a605d7b8
     */
    public void generateScaleId() throws DaoException, IOException {

        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        Map<Integer, Geometry> countries = sdDao.getAllGeometriesInLayer("gadm0", "earth");
        LOG.log(Level.INFO, String.format("Found %d total countries, now loading countries", countries.size()));

        Map<Integer, Geometry> states = sdDao.getAllGeometriesInLayer("gadm1", "earth");
        LOG.log(Level.INFO, String.format("Found %d total states, now loading states", states.size()));

        // initiate scale sets"scaleIds"+CUR_LANG+".txt"
        for (int i=0; i< NUM_SCALES; i++   ){
            scaleIds[i] = new HashSet<Integer>();
        }

        // counter print, for debug updates
        int count = 0;

        for (Integer conceptId : geometries.keySet()) {
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            if (concept == null) {
                System.out.println("Could not find universal page " + conceptId);
                continue;
            }
            LocalPage lpage = lDao.getById(CUR_LANG, concept.getLocalId(CUR_LANG));

            // counter print
            if (DEBUG && count%1000 == 0){
                LOG.log(Level.INFO, "++++++++++++++++++++++++++++++++++ " + count + " ++++++++++++++++++++++++++++++++");

                // exit early
                if (count == 50000){
                    break;
                }
            }
            count++;

            // use getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();

            // break for loop after first proper "instance of" label found for this conceptId
            boolean found = false;

            if (countries.keySet().contains(conceptId)){
                scaleIds[COUNTRY].add(conceptId);
                found = true;
                continue;
            }
            if (states.keySet().contains(conceptId)){
                scaleIds[STATE].add(conceptId);
                found =  true;
                continue;
            }

            // loop over this conceptId's statements
            for (WikidataStatement st : list) {

                //simple English misses some concept local pages
                if (st.getProperty() != null && st.getProperty().getId() == 31) {
                    int id = 0;
                    try {
                        // the id belongs to the "instance of" label
                        id = st.getValue().getIntValue();
                    } catch (NullPointerException e) {
                        System.out.println("Could not get id of \"instance of\" label " + st.getId());
                        continue;
                    }
                    try {
                        // universal id corresponding to "instance of" label
                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        // local page ditto31
                        LocalPage page = lDao.getById(CUR_LANG, concept2.getLocalId(CUR_LANG));

                        // check if there's a spatial keyword in this instance-of label title
                        if (findMatch(conceptId, page.getTitle().toString())) {
                            found = true;
                            break;
                        }

                    } catch (NullPointerException e) {
                        // try to check title in alternate manner because sometimes can't find local page
                        if (wdao.getItem(id).getLabels().get(Language.EN) != null) {
                            if (findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.EN).toString())) {
                                found = true;
                                break;
                            }
                        } else if (wdao.getItem(id).getLabels().get(Language.SIMPLE) != null) {
                            if (findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.SIMPLE).toString())) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            // if didn't find a valid keyword in any of the instance-of descriptions of this concept,
            // do work-arounds based on title
            if (!found) {
                String str = lpage.getTitle().toString();
                // counties get special special treatment
                if (findMatch(conceptId,str)){
                    continue;
                }
                // assume remaining with commas are cities
                else if (str.contains(",")) {
                    scaleIds[CITY].add(conceptId);
                }
                // assume everything else is a landmark
                else {
                    scaleIds[LANDMARK].add(conceptId);
                }
            }
        }

        // write scale ids out to a file
        FileOutputStream fos = new FileOutputStream("scaleIds" + CUR_LANG + ".txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(scaleIds);
        oos.close();
    }

    /**
     * Check if there is a match between the instance-of label and one of the scale keywords.
     * If yes, add it to the appropriate set.
     *
     * @param id              The id of the concept
     * @param instanceOfLabel The label of an instance-of property associated with that concept
     * @return Whether or not the id was placed into a scale set
     */
    private boolean findMatch(int id, String instanceOfLabel) {
        // pre-process the string
        instanceOfLabel = instanceOfLabel.toLowerCase();
        if (instanceOfLabel.endsWith(" (simple)")) {
            instanceOfLabel = instanceOfLabel.substring(0, instanceOfLabel.length() - 9);
        }
        if (instanceOfLabel.endsWith(" (en)")) {
            instanceOfLabel = instanceOfLabel.substring(0, instanceOfLabel.length() - 5);
        }
        if (instanceOfLabel.endsWith(",")) {
            instanceOfLabel = instanceOfLabel.substring(0, instanceOfLabel.length() - 1);
        }



        // check for a match
            for (int i=0; i< NUM_SCALES; i++) {

                if (match(scaleKeywords[i], instanceOfLabel)) {
                    scaleIds[i].add(id);
                    return true;
                }
            }



        // if no match found, return false
        return false;
    }

    /**
     * Check if the pageTitle, or any of its constituent words (space or comma or parenthesis-delimited) are in the given keyword set
     *
     * @param scaleKeywords
     * @param pageTitle
     * @return Whether or not a match was found with the given keyword set
     */
    private boolean match(Set<String> scaleKeywords, String pageTitle) {
        // look for full string in the keyword set
        if (scaleKeywords.contains(pageTitle)) {
            return true;
        }
        // if not there, look for part strings
        else{
//            String[] tokens1 = pageTitle.split(",");
//            pageTitle = tokens1[0];
            int i = pageTitle.indexOf(',');
            if (i>0){
                pageTitle = pageTitle.substring(0,i);
            }
            String[] tokens = pageTitle.split(" ");
            for (String s: tokens){
                if (scaleKeywords.contains(s)) {
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
        for (int i=0; i< NUM_SCALES; i++){
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
    public void printScale(int scaleId) throws DaoException {
        for (Integer conceptId : scaleIds[scaleId]) {
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(CUR_LANG,concept.getLocalId(CUR_LANG));
            String str = lpage.getTitle().toString();
            System.out.println(str.substring(0,str.lastIndexOf('(')-1));
        }
        System.out.println(scaleIds[scaleId].size());
    }

    /**
     * Prints ids of all concepts in a given scale
     *
     * @param scaleId
     * @throws DaoException
     */
    public void printScaleId(int scaleId) throws DaoException {
        for (Integer conceptId : scaleIds[scaleId]) {
            System.out.println(conceptId);
        }
    }

    /**
     * Provides the specified number of concept ids from the given scale set,
     * so that one can check what percentage of them have been placed correctly.
     *
     * @param scaleId
     * @param size
     */
    public void generatePrecisionCalculation(int scaleId, int size) {
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(scaleIds[scaleId]);
        Collections.shuffle(list);
        for (int i = 0; i < size; i++) {
            System.out.println(i + ". " + list.get(i));
        }
    }

    /**
     * Get the set of concepts ids associated with a given scale.
     *
     * @param id
     * @return
     */
    public Set<Integer> getScaleIdSet(int id) {
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
    public void generateRecallTest(int size) throws DaoException {
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(geometries.keySet());
        Collections.shuffle(list);
        for (int i = 0; i< size; i++){
            System.out.print(i + "\t" + list.get(i) + "\t");
            boolean found = false;
            for (int j=0; j<NUM_SCALES; j++){
                if (scaleIds[j].contains(list.get(i))){
                    System.out.println(j+"\t"+fileNames[j]);
                    found = true;
                    break;
                }
            }
            if (!found){
                System.out.println(getScale(list.get(i)));
            }
        }
    }

    public void createScaleFile(){
        Map<Integer, Geometry> geometries =null;
        try {
            geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
            LOG.log(Level.INFO, String.format("Found %d total countries, now loading countries", geometries.size()));
        } catch(DaoException e){
            e.printStackTrace();
        }
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("geometryToScale.txt"));

            for (int i = 0; i<NUM_SCALES; i++){
                bw.write(i+"\t"+fileNames[i]+"\n");
            }
            List<Integer> list = Lists.newArrayList(geometries.keySet());
            for (int i = 0; i<list.size();i++){
                for (int j=0;j< NUM_SCALES;j++){
                    if (scaleIds[j].contains(list.get(i))){
                        bw.write(list.get(i)+"\t"+j+"\n");
                        break;
                    }
                }
            }

            bw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }
}
