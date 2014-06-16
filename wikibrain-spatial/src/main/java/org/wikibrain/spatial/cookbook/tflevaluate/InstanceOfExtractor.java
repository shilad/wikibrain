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
import org.wikibrain.core.jooq.tables.WikidataEntityLabels;
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
    private String[] fileNames = {"country.txt", "state.txt", "city.txt", "natural.txt", "weird.txt"};
    SpatialDataDao sdDao;
    UniversalPageDao upDao ;
    LocalPageDao lDao ;
    WikidataDao wdao ;
    private static final boolean DEBUG = true;

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


    public static void main (String[] args){
        Env env = null;
        InstanceOfExtractor ioe = null;
        try{
            env = EnvBuilder.envFromArgs(args);
            ioe = new InstanceOfExtractor(env);
            ioe.loadScaleKeywords();
            ioe.loadScaleIds(new File("scaleIds.txt"));

            // print out concepts in relevant category
            ioe.printScale(CITY);

        }catch(Exception e){
            System.out.println("Problems");
        }



    }

    /**
     * Load all locations from all language editions of Wikipedia to concepts
     *
     * @throws DaoException, ConfigurationException
     */
    public Set<String> extractInstanceOfList(Env env) throws ConfigurationException,DaoException {

        //setup
        Configurator c = env.getConfigurator();
        SpatialDataDao sdDao = c.get(SpatialDataDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);
        LocalPageDao lDao = c.get(LocalPageDao.class);
        WikidataDao wdao = c.get(WikidataDao.class);

        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        // sets of things that concepts
        HashSet<Integer> set = new HashSet<Integer>();
        HashSet<String> titleSet = new HashSet<String>();
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(Language.SIMPLE,concept.getLocalId(Language.SIMPLE));

            //getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();
            int id2=0;
            for (WikidataStatement st: list){

                //simple English misses some concept local pages
                if (st.getProperty() != null && st.getProperty().getId()==31 ){
                    int id = st.getValue().getIntValue();
                    set.add(id);
                    try {

                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        LocalPage page = lDao.getById(Language.SIMPLE, concept2.getLocalId(Language.SIMPLE));
                        titleSet.add(page.getTitle().toString());
                    } catch(Exception e){
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

    public void loadScaleKeywords() throws FileNotFoundException{
        for (int i = 0; i<MAX-1; i++) {
            scaleKeywords[i] = new HashSet<String>();
            Scanner scanner = new Scanner(new File(fileNames[i]));
            while (scanner.hasNextLine()) {
                scaleKeywords[i].add(scanner.nextLine());
            }
        }
    }

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

    public void generateScaleId() throws DaoException,IOException{
        //setup


        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        for (int i=0; i<MAX; i++   ){
            scaleIds[i] = new HashSet<Integer>();
        }

        int count = 0;
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(Language.SIMPLE,concept.getLocalId(Language.SIMPLE));

            // counter print
            if (DEBUG && count%1000 == 0){
                System.out.println(count);
            }
            count++;

            //getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();
            for (WikidataStatement st: list){
                //simple English misses some concept local pages
                if (st.getProperty() != null && st.getProperty().getId()==31 ){
                    // the id belongs to the "instance of" label
                    int id = st.getValue().getIntValue();
                    try {
                        // universal id corresponding to "instance of" label
                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        // local page ditto
                        LocalPage page = lDao.getById(Language.SIMPLE, concept2.getLocalId(Language.SIMPLE));

                        findMatch(conceptId, page.getTitle().toString());

                    } catch(Exception e){
                        // try to check title in alternate manner
                        if (wdao.getItem(id).getLabels().get(Language.EN)!= null){
                            findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.EN).toString());
                        } else if (wdao.getItem(id).getLabels().get(Language.SIMPLE)!= null) {
                            findMatch(conceptId, wdao.getItem(id).getLabels().get(Language.SIMPLE).toString());
                        }
                    }
                }
            }
        }

        // write scale ids out to a file
        FileOutputStream fos = new FileOutputStream("scaleIds.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(scaleIds);

        oos.close();

    }

    private void findMatch(int id, String string){
        boolean foundMatch = false;
        for (int i=0; i<MAX-1; i++){
            if (match(scaleKeywords[i], string)){
                scaleIds[i].add(id);
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch){
            scaleIds[LANDMARK].add(id);
        }
    }

    private boolean match(Set<String> scaleKeyword, String pageTitle){

        // pre-process the string
        pageTitle = pageTitle.toLowerCase();
        if (pageTitle.endsWith(" (simple)"))
            pageTitle = pageTitle.substring(0,pageTitle.length()-9);

        // look in the set
        if (scaleKeyword.contains(pageTitle)) {
            return true;
        }
        else{
            String[] tokens = pageTitle.split(" ");
            for (String s: tokens){
                if (scaleKeyword.contains(s)) {
                    return true;
                }
            }
        }
        return false;

    }

    public int getScale(int id){
        for (int i=0; i<MAX; i++){
            if (scaleIds[i].contains(id)){
                return i;
            }
        }
        return -1;
    }

    public void printScale( int scaleId) throws DaoException{
        for (Integer conceptId : scaleIds[scaleId]){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(Language.SIMPLE,concept.getLocalId(Language.SIMPLE));
            System.out.println(lpage.getTitle().toString());
        }
    }

    public Set<Integer> getScaleIdSet(int id){
        return scaleIds[id];
    }



}
