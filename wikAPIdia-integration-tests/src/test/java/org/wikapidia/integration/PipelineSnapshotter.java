package org.wikapidia.integration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.download.FileDownloader;
import org.wikapidia.download.RequestedLinkGetter;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * This class creates snapshot information useful for integration tests and stores them
 * in the integration test backup directory.
 *
 * Any "stage" of integration tests can quickly be restored using these backups.
 *
 * @author Shilad Sen
 */
public class PipelineSnapshotter {
    public static String[] DEFAULT_ARGS = {
                    "-c", "integration-test.conf",
                    "-l", "simple,la"
            };

    private final Env env;
    private final DataSource ds;
    private final File dir;

    public PipelineSnapshotter(Env env) throws ConfigurationException {
        this.env = env;
        this.ds = env.getConfigurator().get(DataSource.class);
        this.dir = new File(env.getConfiguration().get().getString("integration.dir"));
    }

    /**
     * Removes any existing backups and creates backups of all stages of the pipeline.
     * @throws Exception
     */
    public void createBackups() throws Exception {
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();

        createDownloads();
    }

    /**
     * Creates the download backup files.
     * @throws InterruptedException
     * @throws WikapidiaException
     * @throws ConfigurationException
     * @throws IOException
     * @throws ParseException
     */
    public void createDownloads() throws InterruptedException, WikapidiaException, ConfigurationException, IOException, ParseException {
        File pathList = new File(env.getConfiguration().get().getString("download.listFile"));
        File pathDownload = new File(env.getConfiguration().get().getString("download.path"));
        FileUtils.deleteQuietly(pathList);
        FileUtils.deleteQuietly(pathDownload);

        RequestedLinkGetter.main(getArgs());
        FileDownloader.main(getArgs());

        FileUtils.copyFile(pathList, new File(dir, "downloadList.tsv"));
        FileUtils.copyDirectory(pathDownload, new File(dir, "download"));
    }

    /**
     * Restores the download backups to their rightful place.
     * @throws IOException
     */
    public void restoreDownloads() throws IOException {
        File pathList = new File(env.getConfiguration().get().getString("download.listFile"));
        File pathDownload = new File(env.getConfiguration().get().getString("download.path"));
        FileUtils.deleteQuietly(pathList);
        FileUtils.deleteQuietly(pathDownload);

        FileUtils.copyFile(new File(dir, "downloadList.tsv"), pathList);
        FileUtils.copyDirectory(new File(dir, "download"), pathDownload);
    }


    public static String[] getArgs(String ...args) {
        return ArrayUtils.addAll(DEFAULT_ARGS, args);
    }

    public static void main(String args[]) throws Exception {
        Env env = new Env(new File("integration-test.conf"));
        PipelineSnapshotter snapshotter = new PipelineSnapshotter(env);
        snapshotter.createBackups();
    }
}
