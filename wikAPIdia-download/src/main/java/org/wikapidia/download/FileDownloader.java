package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.axet.wget.WGet;
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
        tmp = new File("tmp");
    }

    public void downloadFrom(File file) throws IOException {
        if (!tmp.exists()) tmp.mkdir();
        DumpLinkCluster linkCluster = DumpLinkInfo.parseFile(file);
        LOG.log(Level.INFO, "Downloading Files");
        int i = 0;
        for (Multimap<LinkMatcher, DumpLinkInfo> map : linkCluster) {
            for (LinkMatcher linkMatcher : map.keySet()) {
                for (DumpLinkInfo link : map.get(linkMatcher)) {
                    new WGet(link.getUrl(), tmp).download();
                    File download = new File(tmp, link.getDownloadName());
                    download.renameTo(new File(tmp, link.getFileName()));
                    i++;
                    LOG.log(Level.INFO, "Files downloaded: " + (i));
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

    public static void main(String[] args) throws IOException {

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