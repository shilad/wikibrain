package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;

/**
 *
 * @author Ari Weiland
 *
 * Downloads dumps from a specified tsv file containing lines of dump links.
 *
 */

public class FileDownloader {

    private static final Logger LOG = Logger.getLogger(FileDownloader.class.getName());
    private static final int SLEEP_TIME = 10000; // getDump takes a break from downloading
    private static final int MAX_ATTEMPT = 30; // number of attempts before getDump gives up downloading the dump

    private final File tmp;
    private final File output;

    public FileDownloader(File output) {
        this.output = output;
        tmp = new File(".tmp");
    }

    /**
     * Attempts to download the specified file. Returns the success of the download.
     * @param link
     * @return true if successful, else false
     * @throws InterruptedException
     */
    public boolean getDump(DumpLinkInfo link) throws InterruptedException {
        for (int i=0; i<MAX_ATTEMPT; i++) {
            try {
                new WGet(link.getUrl(), tmp).download();
                File download = new File(tmp, link.getDownloadName());
                download.renameTo(new File(tmp, link.getFileName()));
                LOG.log(Level.INFO, "Download complete: " + download.getName());
                Thread.sleep(SLEEP_TIME);
                return true;
            } catch (DownloadIOCodeError e) {
                if (i+1 < MAX_ATTEMPT) {
                    LOG.log(Level.INFO, "Failed to download " + link.getFileName() +
                            ". Reconnecting in " + ((i+1) * (SLEEP_TIME/1000)) + " seconds (HTTP " + e.getCode() + "-Error " + link.getUrl() + ")");
                    Thread.sleep(SLEEP_TIME * (i+1));
                } else {
                    LOG.log(Level.WARNING, "Failed to download " + link.getFileName() + " (HTTP " + e.getCode() + "-Error " + link.getUrl() + ")");
                }
            }
        }
        return false;
    }

    /**
     * Processes a tsv file containing dump link info and initiates the download process
     * on that info. Files are downloaded one language at a time, then one type at a time.
     * Within each language, all of one type is downloaded before moving the files
     * to the destination directory.
     * @param file the tsv file containing the dump link info
     * @throws InterruptedException
     */
    public void downloadFrom(File file) throws InterruptedException {
        if (!tmp.exists()) tmp.mkdir();
        DumpLinkCluster linkCluster = DumpLinkInfo.parseFile(file);
        int numTotalFiles = linkCluster.size();
        LOG.log(Level.INFO, "Starting to download " + numTotalFiles + " files");
        int success = 0;
        int fail = 0;
        for (Multimap<LinkMatcher, DumpLinkInfo> map : linkCluster) {
            for (LinkMatcher linkMatcher : map.keySet()) {
                for (DumpLinkInfo link : map.get(linkMatcher)) {
                    if (getDump(link)) {
                        success++;
                        LOG.log(Level.INFO, success + "/" + numTotalFiles + " file(s) downloaded");
                    } else {
                        fail++;
                    }
                }
                for (DumpLinkInfo link : map.get(linkMatcher)) {
                    File download = new File(tmp, link.getFileName());
                    File target = new File(output, link.getLocalPath());
                    if (!target.exists()) target.mkdirs();
                    download.renameTo(new File(target, download.getName()));
                }
            }
        }
        LOG.log(Level.INFO, success + " files downloaded and " +
                fail + " files failed out of " + numTotalFiles + " files.");
        tmp.delete();
    }

    public static void main(String[] args) throws InterruptedException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("output")
                        .withDescription("Path to output file.")
                        .create("o"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("FileDownloader", options);
            return;
        }

        String filePath = cmd.getOptionValue('o', "download");
        if (cmd.getArgList().isEmpty()) {
            System.err.println("No input files specified.");
            new HelpFormatter().printHelp("FileDownloader", options);
            return;
        }

        final FileDownloader downloader = new FileDownloader(new File(filePath));
        for (Object path : cmd.getArgList()) {
            downloader.downloadFrom(new File((String) path));
        }
    }
}