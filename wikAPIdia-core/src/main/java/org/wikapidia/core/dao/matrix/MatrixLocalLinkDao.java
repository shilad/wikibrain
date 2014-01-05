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
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.matrix.*;
import org.wikapidia.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Wraps a local link dao delegate and builds a fast, sparse, matrix and its
 * transpose to speed up graph lookups.
 *
 * Three API calls are partially supported:
 * 1. The three-argument version of getLinks()
 * 2. get() if a) a language and b) either a src or dest is specified.
 * 3. count() for the same requirements as 2.
 *
 * All other calls are delegated to the passed-in delegate.
 *
 * Note that this dao also loads the links into the delegate.
 *
 * @author Shilad Sen
 */
public class MatrixLocalLinkDao implements LocalLinkDao {
    private static final Logger LOG = Logger.getLogger(MatrixLocalLinkDao.class.getName());

    private final File dir;
    private LocalLinkDao delegate;
    private SparseMatrix matrix = null;
    private SparseMatrix transpose = null;

    // used during building
    private File objectDbPath = null;
    private ObjectDb<int[]> objectDb = null;


    public MatrixLocalLinkDao(LocalLinkDao delegate, File dir) throws DaoException {
        this.delegate = delegate;
        this.dir = dir;
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
            matrix = new SparseMatrix(getMatrixFile());
            transpose = new SparseMatrix(getTransposeFile());
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
            objectDbPath = File.createTempFile("local-links", "odb");
            FileUtils.forceDeleteOnExit(objectDbPath);
            objectDb = new ObjectDb<int[]>(objectDbPath, true);

            // Initialize object database with existing links
            if (matrix != null) {
                for (SparseMatrixRow row : matrix) {
                    int dests[] = new int[row.getNumCols()];
                    for (int i = 0; i < row.getNumCols(); i++) {
                        dests[i] = row.getColIndex(i);
                    }
                    objectDb.put(("" + row.getRowIndex()), dests);
                }
            }
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }
    @Override
    public void save(LocalLink item) throws DaoException {
        delegate.save(item);
        // skip red links
        if (item.getDestId() < 0 || item.getSourceId() < 0) {
            return;
        }
        LocalId src = new LocalId(item.getLanguage(), item.getSourceId());
        LocalId dest = new LocalId(item.getLanguage(), item.getDestId());
        if (!src.canPackInInt() || !dest.canPackInInt()) {
            return;
        }
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
        objectDb.flush();

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
            matrix = new SparseMatrix(getMatrixFile());

            LOG.info("writing transpose of adjacency matrix");

            SparseMatrixTransposer transposer = new SparseMatrixTransposer(matrix, getTransposeFile());
            transposer.transpose();

            LOG.info("loading transpose of adjacency matrix");
            transpose = new SparseMatrix(getTransposeFile());
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
        LocalId id = new LocalId(language, localId);
        if (!id.canPackInInt()) {
            return delegate.getLinks(language, localId, outlinks);
        }
        List<LocalLink> links = new ArrayList<LocalLink>();
        try {
            SparseMatrixRow row = outlinks ? matrix.getRow(id.toInt()) : transpose.getRow(id.toInt());
            if (row == null) {
                return links;
            }
            for (int i = 0; i < row.getNumCols(); i++) {
                LocalId lid = LocalId.fromInt(row.getColIndex(i));
                int srcId = outlinks ? localId : lid.getId();
                int destId = outlinks ? lid.getId() : localId;
                LocalLink ll = new LocalLink(
                        lid.getLanguage(),
                        null,
                        srcId,
                        destId,
                        outlinks,
                        0,
                        true,
                        LocalLink.LocationType.NONE
                );
                links.add(ll);
            }
            return links;
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Iterable<LocalLink> get(DaoFilter daoFilter) throws DaoException {
        // there must be languages
        if (daoFilter.getLangIds() == null) {
            return delegate.get(daoFilter);
        }
        // either source ids or dest ids must be set
        if (daoFilter.getSourceIds() == null && daoFilter.getDestIds() == null) {
            return delegate.get(daoFilter);
        }
        // both must not be set
        if (daoFilter.getSourceIds() != null && daoFilter.getDestIds() != null) {
            return delegate.get(daoFilter);
        }
        // we don't handle location types
        if (daoFilter.getLocTypes() != null || daoFilter.isParseable() != null) {
            return delegate.get(daoFilter);
        }

        // collect link set
        List<LocalLink> links = new ArrayList<LocalLink>();
        int limit = daoFilter.getLimitOrInfinity();
        if (daoFilter.getSourceIds() != null) {
            for (int langId : daoFilter.getLangIds()) {
                for (int srcId : daoFilter.getSourceIds()) {
                    for (LocalLink ll : getLinks(Language.getById(langId), srcId, true)) {
                        links.add(ll);
                        if (links.size() >= limit) break;
                    }
                }
            }
        } else if (daoFilter.getDestIds() != null) {
            for (int langId : daoFilter.getLangIds()) {
                for (int destId : daoFilter.getDestIds()) {
                    for (LocalLink ll : getLinks(Language.getById(langId), destId, false)) {
                        links.add(ll);
                        if (links.size() >= limit) break;
                    }
                }
            }
        }
        return links;
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        // there must be languages
        if (daoFilter.getLangIds() == null) {
            return delegate.getCount(daoFilter);
        }
        // either source ids or dest ids must be set
        if (daoFilter.getSourceIds() == null && daoFilter.getDestIds() == null) {
            return delegate.getCount(daoFilter);
        }
        // both must not be set
        if (daoFilter.getSourceIds() != null && daoFilter.getDestIds() != null) {
            return delegate.getCount(daoFilter);
        }
        // we don't handle location types
        if (daoFilter.getLocTypes() != null || daoFilter.isParseable() != null) {
            return delegate.getCount(daoFilter);
        }

        // collect link count
        try {
            int count = 0;
            if (daoFilter.getSourceIds() != null) {
                List<Integer> packed = getPackedIds(daoFilter);
                if (packed == null) {
                    return delegate.getCount(daoFilter);
                }
                for (int key : packed) {
                    SparseMatrixRow row = matrix.getRow(key);
                    count += (row == null) ? 0 : row.getNumCols();
                }
            } else if (daoFilter.getDestIds() != null) {
                List<Integer> packed = getPackedIds(daoFilter);
                if (packed == null) {
                    return delegate.getCount(daoFilter);
                }
                for (int key : packed) {
                    SparseMatrixRow row = transpose.getRow(key);
                    count += (row == null) ? 0 : row.getNumCols();
                }
            } else {
                throw new IllegalArgumentException();
            }
            return count;
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public LanguageSet getLoadedLanguages() throws DaoException {
        return delegate.getLoadedLanguages();
    }

    public SparseMatrix getMatrix() {
        return matrix;
    }

    public SparseMatrix getTranspose() {
        return transpose;
    }

    private List<Integer> getPackedIds(DaoFilter filter) {
        if (filter.getSourceIds() != null && filter.getDestIds() != null) {
            throw new IllegalArgumentException();
        }
        Collection<Integer> ids = (filter.getSourceIds() != null)
                ? filter.getSourceIds() : filter.getDestIds();
        if (ids == null) {
            throw new IllegalArgumentException();
        }
        List<Integer> packed = new ArrayList<Integer>();
        for (int langId : filter.getLangIds()) {
            for (int id : ids) {
                LocalId lid = new LocalId(Language.getById(langId), id);
                if (!lid.canPackInInt()) {
                    return null;
                }
                packed.add(lid.toInt());
            }
        }
        return packed;
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
        public MatrixLocalLinkDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("matrix")) {
                return null;
            }
            try {
                return new MatrixLocalLinkDao(
                        getConfigurator().get(
                                LocalLinkDao.class,
                                config.getString("delegate")),
                        new File(config.getString("path"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }

        }
    }
}
