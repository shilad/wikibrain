package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.cmd.FileMatcher;
import org.wikapidia.core.lang.Language;
import org.wikapidia.utils.WpIOUtils;

/**
 *
 * Downloads dumps from a specified tsv file containing lines of dump links.
 *
 * @author Ari Weiland
 *
 */

public class DumpFileDownloader {

    private static final Logger LOG = Logger.getLogger(DumpFileDownloader.class.getName());
    private static final int SLEEP_TIME = 10000;    // getOneFile takes a break from downloading
    private static final int MAX_ATTEMPT = 30;      // number of attempts before getOneFile gives up downloading the dump
    private static final int DISPLAY_INFO = 10000;  // amount of time between displaying download progress

    private FileDownloader downloader = new FileDownloader();
    private final File tmpDir;
    private final File outputDir;

    public DumpFileDownloader(File outputDir) {
        this.outputDir = outputDir;
        try {
            tmpDir = WpIOUtils.createTempDirectory("download");
        } catch (IOException e) {
            throw new RuntimeException(e);  // shouldn't happen.
        }
    }

    /**
     * Attempts to download the specified file. Returns the success of the download.
     * @param link
     * @return true if successful, else false
     * @throws InterruptedException
     */
    public File getOneFile(DumpLinkInfo link) throws InterruptedException, IOException {
        return downloader.download(link.getUrl(), new File(tmpDir, link.getFileName()));
    }

    /**
     * Processes a tsv file containing dump link info and initiates the download process
     * on that info. Files are downloaded one language at a time, then one type at a time.
     * Within each language, all of one type is downloaded before moving the files
     * to the destination directory.
     * @param file the tsv file containing the dump link info
     * @throws InterruptedException
     */
    public void downloadFrom(File file) throws InterruptedException, WikapidiaException, IOException {
        if (tmpDir.isDirectory()) {
            if (tmpDir.listFiles().length != 0) {
                for (File f : tmpDir.listFiles()) {
                    f.delete();
                }
            }
        } else {
            tmpDir.mkdirs();
        }
        DumpLinkCluster linkCluster = DumpLinkInfo.parseFile(file);
        int numTotalFiles = linkCluster.size();
        LOG.log(Level.INFO, "Starting to download " + numTotalFiles + " files");
        int success = 0;

        for (Language language : linkCluster) {
            success = downloadLanguageFiles(linkCluster, numTotalFiles, success, language);
        }
        LOG.log(Level.INFO, success + " files downloaded out of " + numTotalFiles + " files.");
        tmpDir.delete();
    }

    private int downloadLanguageFiles(DumpLinkCluster linkCluster, int numTotalFiles, int success, Language language) throws InterruptedException, IOException, WikapidiaException {
        Multimap<FileMatcher, DumpLinkInfo> map = linkCluster.get(language);
        for (FileMatcher linkMatcher : map.keySet()) {
            for (DumpLinkInfo link : map.get(linkMatcher)) {
                File download = new File(outputDir, link.getLocalPath()+"/"+link.getFileName());
                if (download.exists()) {
                    success++;
                    LOG.log(Level.INFO, "File already downloaded: " + link.getFileName());
                } else {
                    download = getOneFile(link);
                    if (download == null) {
                        throw new WikapidiaException("Download malfunction! Download timed out!");
                    }
                    success++;
                    LOG.log(Level.INFO, success + "/" + numTotalFiles + " file(s) downloaded");
                }
                String md5 = DigestUtils.md5Hex(FileUtils.openInputStream(download));
                if (!link.getMd5().equalsIgnoreCase(md5)) {
                    throw new WikapidiaException("Download malfunction! MD5 strings do not match!");
                }
            }
            for (DumpLinkInfo link : map.get(linkMatcher)) {
                File download = new File(tmpDir, link.getFileName());
                File target = new File(outputDir, link.getLocalPath());
                if (!target.exists()) target.mkdirs();
                download.renameTo(new File(target, download.getName()));
            }
        }
        return success;
    }

    public static void main(String[] args) throws ConfigurationException, WikapidiaException, IOException, InterruptedException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("output")
                        .withDescription("Path to output file.")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("input")
                        .withDescription("Path to input tsv file.")
                        .create("i"));

        EnvBuilder.addStandardOptions(options);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpFileDownloader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();

        List argList = Arrays.asList(conf.getConf().get().getString("download.listFile"));
        String filePath = cmd.getOptionValue('o', conf.getConf().get().getString("download.path"));
        if (cmd.hasOption("i")) {
            argList = Arrays.asList(cmd.getOptionValues("i"));
        }

        DumpFileDownloader downloader = new DumpFileDownloader(new File(filePath));
        for (Object path : argList) {
            downloader.downloadFrom(new File((String) path));
        }
    }
}