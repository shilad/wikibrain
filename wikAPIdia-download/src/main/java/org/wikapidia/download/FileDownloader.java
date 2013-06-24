package org.wikapidia.download;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import com.github.axet.wget.WGet;

/**
 * Created with IntelliJ IDEA.
 * User: ilps
 * Date: 6/24/13
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */

public class FileDownloader {


    public static void main(String[] args) {
        try {
            // choise internet url (ftp, http)
            URL url = new URL("http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-site_stats.sql.gz");
            // choise target folder or filename "/Users/axet/Downloads/ap61.ram"
            File target = new File(/*"/Users/axet/Downloads/"*/"Downloads/");
            if (!target.exists()) target.mkdir();
            // initialize wget object
            WGet w = new WGet(url, target);
            // single thread download. will return here only when file download
            // is complete (or error raised).
            w.download();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RuntimeException allDownloadExceptions) {
            allDownloadExceptions.printStackTrace();
        }
    }
}