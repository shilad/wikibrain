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

/**
 *
 * @author Ari Weiland
 *
 * Downloads dumps from a specified tsv file containing lines of dump links.
 *
 */

public class FileDownloader {

    private static final Logger LOG = Logger.getLogger(FileDownloader.class.getName());

    private final File tmp;
    private final File output;

    public FileDownloader(File output) {
        this.output = output;
        tmp = new File(".tmp");
    }

    public void getDump(DumpLinkInfo link, int failedTimes) throws InterruptedException, IOException {
        try {
            new WGet(link.getUrl(), tmp).download();
            File download = new File(tmp, link.getDownloadName());
            download.renameTo(new File(tmp, link.getFileName()));
            LOG.log(Level.INFO, "Download complete: " + download.getName());
            Thread.sleep(5000);
        } catch (DownloadIOCodeError e) {
            failedTimes++;
            LOG.log(Level.INFO, "Fail to download : " + link.getFileName() +
                    ", reconect in " + (failedTimes * 10) + " seconds (HTTP "+ e.getCode() + "-Error " + link.getUrl() + ")");
            Thread.sleep(10000 * failedTimes);
            getDump(link, failedTimes);
        }
    }

    public void downloadFrom(File file) throws IOException, InterruptedException {
        if (!tmp.exists()) tmp.mkdir();
        DumpLinkCluster linkCluster = DumpLinkInfo.parseFile(file);
        int numTotalFiles = linkCluster.size();
        LOG.log(Level.INFO, "Starting to download " + numTotalFiles + " files");
        int i = 0;
        for (Multimap<LinkMatcher, DumpLinkInfo> map : linkCluster) {
            for (LinkMatcher linkMatcher : map.keySet()) {
                for (DumpLinkInfo link : map.get(linkMatcher)) {
                    try {
                        getDump(link, 0);
                        i++;
                        LOG.log(Level.INFO, i + "/" + numTotalFiles + " files downloaded");
                    } catch (DownloadIOCodeError e) {
                        LOG.log(Level.WARNING, "HTTP " + e.getCode() + "-Error at " + link.getUrl());
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
        tmp.delete();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

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