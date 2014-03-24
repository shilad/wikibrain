package org.wikapidia.pageview;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.Title;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 12/1/13
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewIterator implements Iterator {

    private DateTime currentDate;
    private DateTime endDate;
    private LanguageSet langs;
    private static String BASE_URL = "http://dumps.wikimedia.your.org/other/pagecounts-raw/";
    private List<PageViewDataStruct> nextData;

    /**
     * constructs a PageViewIterator and parses a PageViewDataStruct from the first hour input in the constructor,
     * setting nextData to the value of this PageViewDataStruct
     * @param langs
     * @param startDate
     * @param endDate
     * @throws WikapidiaException
     * @throws DaoException
     */
    public PageViewIterator(LanguageSet langs, DateTime startDate, DateTime endDate)
            throws WikapidiaException, DaoException {
        this.langs = langs;
        this.currentDate = startDate;
        if (currentDate.getMillis() < (new DateTime(2007, 12, 9, 18, 0)).getMillis()) {
            throw new WikapidiaException("No page view data supported before 6 PM on 12/09/2007");
        }
        this.endDate = endDate;
    }

    public PageViewIterator(Language lang, DateTime startDate, DateTime endDate)
            throws WikapidiaException, DaoException {
        this.langs = new LanguageSet(lang);
        this.currentDate = startDate;
        if (currentDate.getMillis() < (new DateTime(2007, 12, 9, 18, 0)).getMillis()) {
            throw new WikapidiaException("No page view data supported before 6 PM on 12/09/2007");
        }
        this.endDate = endDate;
    }

    public PageViewIterator(LanguageSet langs, DateTime currentDate){
        this.langs = langs;
        this.currentDate = currentDate;
        this.endDate = currentDate.plusHours(1);
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove not supported for PageViewIterator");
    }

    /**
     * calls hasNext to set the value of nextData to the next hour's PageViewDataStruct
     * @throws NoSuchElementException if hasNext returns false, will occur when the PageViewDataStruct for the last hour in
     * the iterator's range has already been returned
     * @return the value of nextData if it exists
     */
    public List<PageViewDataStruct> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            nextData = getPageViewData();
        }
        catch (ConfigurationException cE){

        }
        catch (WikapidiaException wE) {
            wE.printStackTrace();
        }
        catch (DaoException dE) {
            dE.printStackTrace();
        }
        return nextData;
    }



    /**
     * sets nextData to the PageViewDataStruct of the next hour in this iterator's range
     * called by next() to set the value of next data
     * @return false if nextData is null, true otherwise
     */
    public boolean hasNext() {

        return !(currentDate.getMillis() >= endDate.getMillis());
    }

    /**
     * gets a PageViewDataStruct containing page view info for one hour beginning with current date
     * then increments currentDate by an hour so that this method will get page view info for the next hour next time it's called
     * @return PageViewDataStruct for the current hour, or null if currentDate isn't more recent than endDate
     * @throws WikapidiaException
     * @throws DaoException
     */
    private List<PageViewDataStruct> getPageViewData() throws WikapidiaException, DaoException, ConfigurationException {
        if (currentDate.getMillis() >= endDate.getMillis()) {
            return null;
        }

        //set up temp folder where page view data file will be stored
        File tempFolder = new File("../download/" + "_page_view_data");
        if (!tempFolder.exists()){
            tempFolder.mkdir();
        }

        // build up the file name for the page view data file from the current date
        String yearString = ((Integer) currentDate.getYear()).toString();
        String monthString = twoDigIntStr(currentDate.getMonthOfYear());
        String dayString = twoDigIntStr(currentDate.getDayOfMonth());
        String hourString = twoDigIntStr(currentDate.getHourOfDay());
        String fileNameSuffix = ".gz";

        String homeFolder = BASE_URL + String.format("%s/%s-%s/", yearString, yearString, monthString);
        File pageViewDataFile = null;
        int minutes = 0;
        while (pageViewDataFile == null && minutes < 60) {
            int seconds = 0;
            boolean flag = false;
            while (pageViewDataFile == null && seconds < 60) {
                String minutesString = twoDigIntStr(minutes);
                String secondsString = twoDigIntStr(seconds);
                String fileName = "pagecounts-" + yearString + monthString + dayString + "-" + hourString + minutesString + secondsString + fileNameSuffix;
                pageViewDataFile = downloadFile(homeFolder, fileName, tempFolder);
                if(pageViewDataFile != null){
                    flag = true;
                    break;
                }
                seconds++;
            }
            if(flag)
                break;
            minutes++;
        }
        if(pageViewDataFile == null) {
            DateTime tempDate = currentDate;
            currentDate = currentDate.plusHours(1);
            throw new WikapidiaException("null pageViewDataFile for date " + tempDate);
        }

        List<PageViewDataStruct> dataStructs = new ArrayList<PageViewDataStruct>();
        DateTime nextDate = currentDate.plusHours(1);
        for (Language lang : langs) {
            TIntIntMap pageViewCounts = parsePageViewDataFromFile(lang, pageViewDataFile);
            PageViewDataStruct pageViewData = new PageViewDataStruct(lang, currentDate, nextDate, pageViewCounts);
            dataStructs.add(pageViewData);
        }

        pageViewDataFile.delete();
        tempFolder.delete();

        currentDate = nextDate;
        return dataStructs;
    }

    private static String twoDigIntStr(int time){
        String rVal = Integer.toString(time);
        if (time < 10){
            rVal = "0" + rVal;
        }
        return rVal;
    }

    private static File downloadFile(String folderUrl, String fileName, File localFolder){

        try{
            System.out.println("Downloading Pageview File");
            URL url = new URL(folderUrl + fileName);
            String localPath = localFolder.getAbsolutePath() + "/" + fileName;
            File dest = new File(localPath);
            if(!dest.exists()){
                System.out.println("File not exist. Downloading...");
                FileUtils.copyURLToFile(url, dest, 60000, 120000);
            }
            else{
                System.out.println("File existed. Skip...");
            }
            File ungzipDest = new File(localPath + ".txt");
            if(!ungzipDest.exists()){
                ungzip(dest,ungzipDest);
            }
            System.out.println("Finished Downloading Pageview File");
            dest.delete();
            return ungzipDest;
        } catch(IOException e) {
            System.out.println("\nFile name " + fileName + " couldn't be found online");
            System.out.println(e.getMessage() + "\n");
            return null;
        }

    }

    private static void ungzip(File inFile, File outFile) throws IOException{

        BufferedInputStream gbin = new BufferedInputStream(new GZIPInputStream(new FileInputStream(inFile)));
        FileUtils.copyInputStreamToFile(gbin, outFile);
        gbin.close();
    }

    private static TIntIntMap parsePageViewDataFromFile(Language lang, File f) throws WikapidiaException, DaoException, ConfigurationException {

        try{
            Env env = new EnvBuilder().build();
            TIntIntMap data = new TIntIntHashMap();
            Configurator configurator = env.getConfigurator();
            LocalPageDao pdao = configurator.get(LocalPageDao.class, "sql");

            BufferedReader br =  new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String curLine;
            System.out.println("Beginning to parse file");
            while ((curLine = br.readLine()) != null) {
                String[] cols = curLine.split(" ");
                if (cols.length < 3) {
                    continue;
                }
                if (cols[0].equals(lang.getLangCode())) {
                    try{
                        String title = URLDecoder.decode(cols[1], "UTF-8");
                        int pageId = pdao.getIdByTitle(new Title(title, lang));
                        int numPageViews = Integer.parseInt(cols[2]);
                        data.adjustOrPutValue(pageId, numPageViews, numPageViews);
                    }
                    catch(IllegalArgumentException e){
                        System.out.println("Decoding error examining this line: " + curLine);
                    }
                    catch(DaoException de) {
                        System.out.println("Error using page DAO to get page ID for line:\n\t" + curLine);
                        System.out.println(de.getMessage());
                    }
                }
            }
            br.close();

            return data;
        }
        catch(IOException e){
            throw new WikapidiaException(e);
        }

    }

}
