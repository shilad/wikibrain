package org.wikibrain.pageview;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.download.FileDownloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Shilad Sen
 */
public class PageViewDownloader {
    private static String BASE_URL = "http://dumps.wikimedia.org/other/pagecounts-raw/";
    private static Logger LOG = LoggerFactory.getLogger(PageViewDownloader.class);

    private final File dir;

    public PageViewDownloader(File dir) {
        this.dir = dir;
        dir.mkdirs();
    }

    public TreeMap<DateTime, File> download(DateTime startDate, int numHours) throws WikiBrainException {
        return download(startDate, startDate.plusHours(numHours));
    }

    public TreeMap<DateTime, File> download(DateTime startDate, DateTime endDate) throws WikiBrainException {
        return download(PageViewUtils.timestampsInInterval(startDate, endDate));
    }

    public TreeMap<DateTime, File> download(SortedSet<DateTime> timestamps) throws WikiBrainException {
        TreeMap<DateTime, File> files = new TreeMap<DateTime, File>();
        for (DateTime current : timestamps) {
            File file = downloadOne(current);
            if (file == null) {
                LOG.info("Did not find a pageview file for date " + current);
            } else {
                files.put(current, file);
            }
            current = current.plusHours(1);
        }
        return files;
    }

    /**
     * Downloads a single file that must already exist.
     * @param tstamp
     * @return Filename, or null if it does not exist.
     * @throws WikiBrainException
     */
    private File downloadOne(DateTime tstamp) throws WikiBrainException {

        // build up the file name for the page view data file from the current date
        String yearString = ((Integer) tstamp.getYear()).toString();
        String monthString = twoDigIntStr(tstamp.getMonthOfYear());
        String dayString = twoDigIntStr(tstamp.getDayOfMonth());
        String hourString = twoDigIntStr(tstamp.getHourOfDay());
        String fileNameSuffix = ".gz";

        File dest = new File(dir,
                String.format("%s/%s/%s-%s-%s-%s:00.gz",
                        yearString, monthString,
                        yearString, monthString, dayString, hourString));

        String homeFolder = BASE_URL + String.format("%s/%s-%s/", yearString, yearString, monthString);
        for (int minutes = 0; minutes < 60; minutes++) {
            for (int seconds = 0; seconds < 60; seconds++) {
                String minutesString = twoDigIntStr(minutes);
                String secondsString  = twoDigIntStr(seconds);
                String f = "pagecounts-" + yearString + monthString + dayString + "-" + hourString + minutesString + secondsString + fileNameSuffix;
                String url = homeFolder + f;
                if (ping(url, 5000)) {
                    return downloadFile(url, dest);
                }
            }
        }

        return null;
    }

    private File downloadFile(String urlStr, File dest){
        if (dest.exists()) {
            LOG.info("Skipping existing pageview file " + dest);
            return dest;
        }
        LOG.info("Downloading pageview url " + urlStr + " to " + dest);
        try{
            URL url = new URL(urlStr);
            File tmp = File.createTempFile("pageview", ".gz");
            FileDownloader downloader = new FileDownloader();
            downloader.download(url, tmp);
            dest.getParentFile().mkdirs();
            FileUtils.deleteQuietly(dest);
            FileUtils.moveFile(tmp, dest);
            return dest;
        } catch(IOException e) {
            LOG.warn("downloading of file " + urlStr + " failed: ", e);
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String twoDigIntStr(int time){
        String rVal = Integer.toString(time);
        if (time < 10){
            rVal = "0" + rVal;
        }
        return rVal;
    }


    /**
     * From http://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-a-http-url-for-availability
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in
     * the 200-399 range.
     * @param url The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     * given timeout, otherwise <code>false</code>.
     */
    public static boolean ping(String url, int timeout) {
        url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.
        HttpURLConnection connection = null;
        try {
            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            int code = connection.getResponseCode();
            return (200 <= code && code <= 399);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
