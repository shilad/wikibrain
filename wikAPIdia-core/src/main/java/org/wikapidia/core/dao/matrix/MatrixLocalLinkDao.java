package org.wikapidia.core.dao.matrix;

import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.matrix.*;
import org.wikapidia.utils.ObjectDb;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Wraps a local link dao delegate and builds a fast, sparse, matrix and its
 * transpose to speed up graph lookups.
 *
 * @author Shilad Sen
 */
public class MatrixLocalLinkDao implements LocalLinkDao {
    private static final Logger LOG = Logger.getLogger(MatrixLocalLinkDao.class.getName());

    private final File dir;
    private final int maxPageSize;
    private final int maxOpenPages;
    private LocalLinkDao delegate;
    private SparseMatrix matrix = null;
    private SparseMatrix transpose = null;

    // used during building
    private File objectDbPath = null;
    private ObjectDb<int[]> objectDb = null;

    public MatrixLocalLinkDao(LocalLinkDao delegate, File dir) throws DaoException {
        this(delegate, dir, 10, 100*1024*1024);     // 5 pages, 100MB each, for both matrix and transpose
    }

    public MatrixLocalLinkDao(LocalLinkDao delegate, File dir, int maxOpenPages, int maxPageSize) throws DaoException {
        this.delegate = delegate;
        this.dir = dir;
        this.maxOpenPages = maxOpenPages;
        this.maxPageSize = maxPageSize;
        dir.mkdirs();
        try {
            load();
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    private void load() throws IOException {
        if (!getMatrixFile().isFile()) {
            LOG.warning("Matrix" + getMatrixFile()+ " missing, disabling fast lookups.");
        } else if (!getTransposeFile().isFile()) {
            LOG.warning("Matrix" + getTransposeFile()+ " missing, disabling fast lookups.");
        } else {
            matrix = new SparseMatrix(getMatrixFile(), maxOpenPages, maxPageSize);
            transpose = new SparseMatrix(getTransposeFile(), maxOpenPages, maxPageSize);
        }
    }

    @Override
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException {
        return delegate.getLink(language, sourceId, destId);
    }

    @Override
    public void beginLoad() throws DaoException {
        delegate.beginLoad();
        try {
            File f = new File(".tmp");
            f.mkdirs();
            objectDbPath = File.createTempFile("local-links", "odb", f);
            FileUtils.forceDeleteOnExit(objectDbPath);
            objectDb = new ObjectDb<int[]>(objectDbPath, true);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }
    @Override
    public void save(LocalLink item) throws DaoException {
        delegate.save(item);
        LocalId src = new LocalId(item.getLanguage(), item.getSourceId());
        LocalId dest = new LocalId(item.getLanguage(), item.getDestId());
        if (!src.canPackInInt() || !dest.canPackInInt()) {
            System.err.println("here1");
            return;
        }
        System.err.println("here2");
        try {
            String key = "" + src.toInt();
            int[] val = objectDb.get(key);
            objectDb.put(key, ArrayUtils.add(val, dest.toInt()));
        } catch (IOException e) {
           throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    public File getMatrixFile() {
        return new File(dir, "links.matrix");
    }

    public File getTransposeFile() {
        return new File(dir, "links-transpose.matrix");
    }

    @Override
    public void clear() throws DaoException {
        delegate.clear();
        FileUtils.deleteQuietly(getMatrixFile());
        FileUtils.deleteQuietly(getTransposeFile());
    }

    @Override
    public void endLoad() throws DaoException {
        delegate.endLoad();

        try {
            LOG.info("writing adjacency matrix rows");
            ValueConf vconf = new ValueConf();   // unused because there are no values.
            SparseMatrixWriter writer = new SparseMatrixWriter(getMatrixFile(), vconf);
            for (Pair<String, int[]> entry : objectDb) {
                SparseMatrixRow row = new SparseMatrixRow(
                        vconf,
                        Integer.valueOf(entry.getKey()),
                        entry.getValue(),
                        new short[entry.getValue().length]
                );
                writer.writeRow(row);
            }
            LOG.info("finalizing adjacency matrix");
            writer.finish();

            LOG.info("finalizing adjacency matrix");
            matrix = new SparseMatrix(getMatrixFile(), maxOpenPages, maxPageSize);

            LOG.info("writing transpose of adjacency matrix");
            SparseMatrixTransposer transposer = new SparseMatrixTransposer(matrix, getTransposeFile(), maxOpenPages * maxPageSize);
            transposer.transpose();

            LOG.info("loading transpose of adjacency matrix");
            transpose = new SparseMatrix(getTransposeFile(), maxOpenPages, maxPageSize);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException {
        return delegate.getLinks(language, localId, outlinks, isParseable, locationType);
    }

    @Override
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException {
        return delegate.getLinks(language, localId, outlinks);
    }

    @Override
    public Iterable<LocalLink> get(DaoFilter daoFilter) throws DaoException {
        return delegate.get(daoFilter);
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        return delegate.getCount(daoFilter);
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalLinkDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LocalLinkDao> getType() {
            return LocalLinkDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localLink";
        }

        @Override
        public MatrixLocalLinkDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("matrix")) {
                return null;
            }
            try {
                return new MatrixLocalLinkDao(
                        getConfigurator().get(
                                LocalLinkDao.class,
                                config.getString("delegate")),
                        new File(config.getString("path")),
                        config.getInt("maxOpenPages"),
                        config.getBytes("maxPageSize").intValue()
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }

        }
    }
}
