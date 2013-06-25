package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.axet.wget.WGet;
import org.apache.commons.cli.*;
import org.wikapidia.conf.DefaultOptionBuilder;

/**
 */

public class FileDownloader {

    private final File tmp;
    private final File output;

    public FileDownloader(File output) {
        this.output = output;
        tmp = new File("tmp");
    }

    public void downloadFrom(File file) throws IOException {
        if (!tmp.exists()) tmp.mkdir();
        List<DumpLinkInfo> links = DumpLinkInfo.parseFile(file);
        System.out.println("Downloading Files");
        for (int i=0; i<links.size(); i++) {
            DumpLinkInfo link = links.get(i);
            File target = new File(output, link.getLocalPath());
            if (!target.exists()) target.mkdirs();
            new WGet(link.getUrl(), tmp).download();
            System.out.println("Files downloaded: " + (i+1));
            File download = tmp.listFiles()[0];
            download.renameTo(new File(target, link.getFileName()));
        }
        tmp.delete();
    }

    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
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

        String filePath = cmd.getOptionValue('o');
        File file = new File(filePath);
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