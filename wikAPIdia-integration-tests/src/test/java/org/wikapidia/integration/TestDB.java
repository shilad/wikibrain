package org.wikapidia.integration;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Restore;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.dao.load.DumpLoader;
import org.wikapidia.dao.load.LuceneLoader;
import org.wikapidia.dao.load.RedirectLoader;
import org.wikapidia.dao.load.WikiTextLoader;
import org.wikapidia.download.DumpFileDownloader;
import org.wikapidia.download.RequestedLinkGetter;
import org.wikapidia.utils.ZipDir;

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

    private Env env;
    private WpDataSource ds;
    private final File dir;


    public TestDB(Env env) throws ConfigurationException {
        this.env = env;
        this.ds = env.getConfigurator().get(WpDataSource.class);
        this.dir = new File(env.getConfiguration().get().getString("integration.dir"));
    }

    /**
     * Removes any existing backups and creates backups of all stages of the pipeline.
     * @throws Exception
     */
    public void createBackups() throws Exception {
       shutdownH2();
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();

        createDownloads();
        createRawAndLocal();
        createRedirect();
        createWikiText();
        createLucene();
    }

    /**
     * Creates the download backup files.
     * @throws InterruptedException
     * @throws WikapidiaException
     * @throws ConfigurationException
     * @throws IOException
     * @throws ParseException
     */
    private void createDownloads() throws InterruptedException, WikapidiaException, ConfigurationException, IOException, ParseException {
        File pathList = new File(env.getConfiguration().get().getString("download.listFile"));
        File pathDownload = new File(env.getConfiguration().get().getString("download.path"));
        FileUtils.deleteQuietly(pathList);
        FileUtils.deleteQuietly(pathDownload);

        RequestedLinkGetter.main(TestUtils.getArgs());
        DumpFileDownloader.main(TestUtils.getArgs());

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

    private void createRawAndLocal() throws ClassNotFoundException, SQLException, DaoException, ConfigurationException, IOException {
        deleteH2Backup("rawAndLocal.zip");
        DumpLoader.main(TestUtils.getArgs("-d"));
        backupH2To("rawAndLocal.zip");
    }

    public void restoreRawAndLocal() throws SQLException, ConfigurationException {
        restoreH2From("rawAndLocal.zip");
    }

    private void createRedirect() throws DaoException, ConfigurationException, SQLException {
        deleteH2Backup("redirect.zip");
        RedirectLoader.main(TestUtils.getArgs("-d"));
        backupH2To("redirect.zip");
    }

    public void restoreRedirect() throws SQLException, ConfigurationException {
        restoreH2From("redirect.zip");
    }

    private void createWikiText() throws IOException, DaoException, ConfigurationException, SQLException {
        deleteH2Backup("wikitext.zip");
        WikiTextLoader.main(TestUtils.getArgs("-d"));
        backupH2To("wikitext.zip");
    }


    public void restoreWikiText() throws SQLException, ConfigurationException {
        restoreH2From("wikitext.zip");
    }

    private void createLucene() throws DaoException, WikapidiaException, ConfigurationException, IOException, SQLException {
        File luceneDir = new File(env.getConfiguration().get().getString("lucene.directory"));
        deleteH2Backup("lucene.zip");
        FileUtils.deleteQuietly(luceneDir);
        LuceneLoader.main(TestUtils.getArgs());
        ZipDir.zip(luceneDir, new File(dir, "lucene-dir.zip"));
        backupH2To("lucene.zip");
    }

    public void restoreLucene() throws SQLException, ConfigurationException, IOException {
        File luceneDir = new File(env.getConfiguration().get().getString("lucene.directory"));
        ZipDir.unzip(new File(dir, "lucene-dir.zip"), luceneDir);
        restoreH2From("lucene.zip");
    }

    private void deleteH2Backup(String filename) {
        FileUtils.deleteQuietly(new File(dir, filename));
    }

    private void backupH2To(String fileName) throws SQLException {
        Connection cnx = ds.getConnection();
        try {
            cnx.createStatement().execute("BACKUP TO '" + new File(dir, fileName) + "'");
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }

    private void restoreH2From(String fileName) throws SQLException, ConfigurationException {
        LOG.info("restoring " + fileName);
        shutdownH2();
        LOG.info("finished shutting down ");
        Restore.main(
                "-file", new File(dir, fileName).toString(),
                "-dir", env.getConfiguration().get().getString("baseDir") + "/db",
                "-db", "h2"
        );
        LOG.info("finished restoring " + fileName);
    }

    private void shutdownH2() throws SQLException, ConfigurationException {
        Connection cnx = ds.getConnection();
        try {
            cnx.createStatement().execute("SHUTDOWN IMMEDIATELY");
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
        // Reset the environment because the database is no longer available.
        env = TestUtils.getEnv();
        ds = env.getConfigurator().get(WpDataSource.class);
    }

    public Env getEnv() {
        return env;
    }

    public static void main(String args[]) throws Exception {
        TestDB snapshotter = new TestDB(TestUtils.getEnv());
        snapshotter.createBackups();
    }
}
