package org.wikapidia.integration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.dao.load.DumpLoader;
import org.wikapidia.dao.load.RedirectLoader;
import org.wikapidia.download.FileDownloader;
import org.wikapidia.download.RequestedLinkGetter;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * This class creates snapshot information useful for integration tests and stores them
 * in the integration test backup directory.
 *
 * Any "stage" of integration tests can quickly be restored using these backups.
 *
 * @author Shilad Sen
 */
public class TestDB {
    private static final Logger LOG = Logger.getLogger(TestDB.class.getName());

    private final Env env;
    private final DataSource ds;
    private final File dir;


    public TestDB(Env env) throws ConfigurationException {
        this.env = env;
        this.ds = env.getConfigurator().get(DataSource.class);
        this.dir = new File(env.getConfiguration().get().getString("integration.dir"));
    }

    /**
     * Removes any existing backups and creates backups of all stages of the pipeline.
     * @throws Exception
     */
    public void createBackups() throws Exception {
/*
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();

        createDownloads();
        createRawAndLocal();

        */
        createRedirect();
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

        RequestedLinkGetter.main(TestUtils.getArgs());
        FileDownloader.main(TestUtils.getArgs());

        FileUtils.copyFile(pathList, new File(dir, "downloadList.tsv"));
        FileUtils.copyDirectory(pathDownload, new File(dir, "download"));
    }

    /**
        String filePath = conf.getConf().get().getString("download.listFile");
     * Restores the download backups to their rightful place.
     * @throws IOException
     */
    public void restoreDownloads() throws IOException {
        File pathList = new File(env.getConfiguration().get().getString("download.listFile"));
        File pathDownload = new File(env.getConfiguration().get().getString("download.path"));
        FileUtils.deleteQuietly(pathList);
        FileUtils.deleteQuietly(pathDownload);

        FileUtils.copyFile(new File(dir, "download/list.tsv"), pathList);
        FileUtils.copyDirectory(new File(dir, "download"), pathDownload);
    }

    public void createRawAndLocal() throws ClassNotFoundException, SQLException, DaoException, ConfigurationException, IOException {
        deleteH2Backup("rawAndLocal.sql");
        DumpLoader.main(TestUtils.getArgs("-d"));
        backupH2To("rawAndLocal.sql");
    }

    public void restoreRawAndLocal() throws SQLException {
        restoreH2From("rawAndLocal.sql");
    }

    public void createRedirect() throws DaoException, ConfigurationException, SQLException {
        deleteH2Backup("redirect.sql");
        RedirectLoader.main(TestUtils.getArgs("-d"));
        backupH2To("redirect.sql");
    }

    public void restoreRedirect() throws SQLException {
        restoreH2From("redirect.sql");
    }

    private void deleteH2Backup(String filename) {
        FileUtils.deleteQuietly(new File(dir, filename));
    }

    private void backupH2To(String fileName) throws SQLException {
        Connection cnx = ds.getConnection();
        try {
            cnx.createStatement().execute("SCRIPT TO '" + new File(dir, fileName) + "' COMPRESSION LZF");
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }

    private void restoreH2From(String fileName) throws SQLException {
        LOG.info("restoring " + fileName);
        Connection cnx = ds.getConnection();
        try {
            cnx.createStatement().execute("DROP ALL OBJECTS; RUNSCRIPT FROM '" + new File(dir, fileName) + "' COMPRESSION LZF");
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
        LOG.info("finished restoring " + fileName);
    }

    public Env getEnv() {
        return env;
    }

    public static void main(String args[]) throws Exception {
        TestDB snapshotter = new TestDB(TestUtils.getEnv());
        snapshotter.createBackups();
    }
}
