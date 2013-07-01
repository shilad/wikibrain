package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.lang.Language;

/**
 *
 * @author Ari Weiland
 *
 * Downloads dumps from a specified tsv file containing lines of dump links.
 *
 */

public class FileDownloader {

    private static final Logger LOG = Logger.getLogger(FileDownloader.class.getName());
    private static final int SLEEP_TIME = 10000;    // getDump takes a break from downloading
    private static final int MAX_ATTEMPT = 30;      // number of attempts before getDump gives up downloading the dump
    private static final int DISPLAY_INFO = 10000;  // amount of time between displaying download progress

    private final File tmp = new File(".tmp").getAbsoluteFile();
    private final File output;

    private DownloadInfo info;

    public FileDownloader(File output) {
        this.output = output;
    }

    /**
     * Attempts to download the specified file. Returns the success of the download.
     * @param link
     * @return true if successful, else false
     * @throws InterruptedException
     */
    public File getDump(DumpLinkInfo link) throws InterruptedException, IOException {
        for (int i=0; i < MAX_ATTEMPT; i++) {
            try {
                AtomicBoolean stop = new AtomicBoolean(false);
                File download = new File(tmp, link.getFileName());
                info = new DownloadInfo(link.getUrl());
                info.extract(stop, notify);
                new WGet(info, download).download(stop, notify);
                LOG.log(Level.INFO, "Download complete: " + download.getName());
                Thread.sleep(SLEEP_TIME);
                return download;
            } catch (DownloadIOCodeError e) {
                if (i+1 < MAX_ATTEMPT) {
                    LOG.log(Level.INFO, "Failed to download " + link.getFileName() +
                            ". Reconnecting in " + ((i+1) * (SLEEP_TIME/1000)) +
                            " seconds (HTTP " + e.getCode() + "-Error " + link.getUrl() + ")");
                    Thread.sleep(SLEEP_TIME * (i+1));
                } else {
                    LOG.log(Level.WARNING, "Failed to download " + link.getFileName() +
                            " (HTTP " + e.getCode() + "-Error " + link.getUrl() + ")");
                }
            }
        }
        return null;
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
        if (tmp.isDirectory()) {
            if (tmp.listFiles().length != 0) {
                for (File f : tmp.listFiles()) {
                    f.delete();
                }
            }
        } else {
            tmp.mkdirs();
        }
        DumpLinkCluster linkCluster = DumpLinkInfo.parseFile(file);
        int numTotalFiles = linkCluster.size();
        LOG.log(Level.INFO, "Starting to download " + numTotalFiles + " files");
        int success = 0;
        for (Language language : linkCluster) {
            Multimap<LinkMatcher, DumpLinkInfo> map = linkCluster.get(language);
            for (LinkMatcher linkMatcher : map.keySet()) {
                for (DumpLinkInfo link : map.get(linkMatcher)) {
                    File download = new File(output, link.getLocalPath()+"/"+link.getFileName());
                    if (download.exists()) {
                        success++;
                        LOG.log(Level.INFO, "File already downloaded: " + link.getFileName());
                    } else {
                        download = getDump(link);
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
                    File download = new File(tmp, link.getFileName());
                    File target = new File(output, link.getLocalPath());
                    if (!target.exists()) target.mkdirs();
                    download.renameTo(new File(target, download.getName()));
                }
            }
        }
        LOG.log(Level.INFO, success + " files downloaded out of " + numTotalFiles + " files.");
        tmp.delete();
    }

    public static void main(String[] args) throws ConfigurationException, WikapidiaException, IOException, InterruptedException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("output")
                        .withDescription("Path to output file.")
                        .create("o"));

        Env.addStandardOptions(options);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("FileDownloader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();

        List argList = Arrays.asList(conf.getConf().get().getString("download.listFile"));
        String filePath = cmd.getOptionValue('o', conf.getConf().get().getString("download.path"));
        if (!cmd.getArgList().isEmpty()) {
            argList = cmd.getArgList();
        }

        FileDownloader downloader = new FileDownloader(new File(filePath));
        for (Object path : argList) {
            downloader.downloadFrom(new File((String) path));
        }
    }

    private Runnable notify = new Runnable() {
        long last;

        @Override
        public void run() {
            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                case DONE:
                    LOG.log(Level.INFO, "" + info.getState());
                    break;
                case RETRYING:
                    LOG.log(Level.INFO, info.getState() + " " + info.getDelay());
                    break;
                case DOWNLOADING:
                    long now = System.currentTimeMillis();
                    if (now > last + DISPLAY_INFO) {
                        last = now;
                        LOG.log(Level.INFO, String.format("%s %d %%", info.getSource(), info.getCount() * 100 / info.getLength()));
                    }
                    break;
                default:
                    break;
            }
        }
    };
}