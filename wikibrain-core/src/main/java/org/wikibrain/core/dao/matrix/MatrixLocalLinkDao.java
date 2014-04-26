package org.wikibrain.core.dao.matrix;

import com.google.code.externalsorting.ExternalSort;
import com.typesafe.config.Config;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.matrix.*;
import org.wikibrain.utils.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private Set<File> allWriterFiles = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());
    private Set<BufferedWriter> allWriters = Collections.newSetFromMap(
            new ConcurrentHashMap<BufferedWriter, Boolean>());
    private ThreadLocal<BufferedWriter> writers = new ThreadLocal<BufferedWriter>();


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
        allWriters.clear();
        delegate.beginLoad();
        // Initialize object database with existing links
        if (matrix != null) {
            ParallelForEach.iterate(matrix.iterator(), new Procedure<SparseMatrixRow>() {
                @Override
                public void call(SparseMatrixRow row) throws Exception {
                    BufferedWriter writer = getSortingWriter();
                    for (int i = 0; i < row.getNumCols(); i++) {
                        writer.write(row.getRowIndex() + " " + row.getColIndex(i) + "\n");
                    }
                }
            });
        }
    }

    private BufferedWriter getSortingWriter() throws IOException {
        if (writers.get() == null) {
            File file = File.createTempFile("links-sorter", ".txt");
            file.deleteOnExit();
            file.delete();
            writers.set(WpIOUtils.openWriter(file));
            allWriters.add(writers.get());
            allWriterFiles.add(file);
        }
        return writers.get();
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
            BufferedWriter writer = getSortingWriter();
            writer.write(src.toInt() + " " + dest.toInt() + "\n");
        } catch (IOException e) {
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



    private static final int MAX_SORT_THREADS = 4;
    private File sortFiles() throws IOException {
        for (BufferedWriter writer : allWriters) {
            writer.close();
        }
        ParallelForEach.iterate(allWriterFiles.iterator(), MAX_SORT_THREADS, 10, new Procedure<File>() {
            @Override
            public void call(File file) throws Exception {
                sort(file, MAX_SORT_THREADS);
            }
        }, 10);

        File file = File.createTempFile("local-links-sorted.", ".txt");
        file.deleteOnExit();
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                return r1.compareTo(r2);}};
        LOG.info("merging all sorted files to " + file);
        ExternalSort.mergeSortedFiles(new ArrayList<File>(allWriterFiles), file, comparator, Charset.forName("utf-8"));
        return file;
    }

    private static final int SORT_FILES_MAX = 100;
    private static final long SORT_MEMORY_MAX = (Runtime.getRuntime().maxMemory() / MAX_SORT_THREADS / 5);

    private void sort(File file, long concurrentThreads) throws IOException {
        long maxMemory = SORT_MEMORY_MAX / concurrentThreads;
        int maxFiles = (int) Math.max(
                SORT_FILES_MAX / concurrentThreads,
                (int)(file.length() / (maxMemory / 2)));
        LOG.info("sorting " + file + " using max of " + maxFiles);
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                return r1.compareTo(r2);}};
        List<File> l = ExternalSort.sortInBatch(
                WpIOUtils.openBufferedReader(file),
                file.length(),
                comparator,
                maxFiles,
                maxMemory,
                Charset.forName("utf-8"),
                null,
                true,
                0,
                false);
        LOG.info("merging " + file);
        ExternalSort.mergeSortedFiles(l, file, comparator, Charset.forName("utf-8"));
        LOG.info("finished sorting" + file);
    }

    @Override
    public void endLoad() throws DaoException {
        delegate.endLoad();

        try {
            // close the old matrix and transpose
            LOG.info("closing existing matrix and transpose.");
            if (matrix != null) IOUtils.closeQuietly(matrix);
            if (transpose != null) IOUtils.closeQuietly(transpose);

            LOG.info("sorting files");
            File file = sortFiles();

            LOG.info("writing adjacency matrix rows");
            ValueConf vconf = new ValueConf();   // unused because there are no values.
            SparseMatrixWriter writer = new SparseMatrixWriter(getMatrixFile(), vconf);

            BufferedReader reader = WpIOUtils.openBufferedReader(file);
            TIntList packedDest = new TIntArrayList();

            int cellCount = 0;
            int rowCount = 0;
            LocalId lastSrc = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String tokens[] = line.trim().split(" ");
                if (tokens.length == 2){
                    cellCount++;
                    LocalId src = LocalId.fromInt(Integer.valueOf(tokens[0]));
                    LocalId dest = LocalId.fromInt(Integer.valueOf(tokens[1]));
                    if (lastSrc != null && !src.equals(lastSrc)) {
                        if (++rowCount % 100000 == 0) {
                            LOG.info("writing adjacency matrix row " + rowCount
                                    + ", found " + cellCount + " links");
                        }
                        SparseMatrixRow row = new SparseMatrixRow(
                                vconf,
                                lastSrc.toInt(),
                                packedDest.toArray(),
                                new short[packedDest.size()]
                        );
                        writer.writeRow(row);
                        packedDest.clear();
                    }
                    packedDest.add(dest.toInt());
                    lastSrc = src;
                } else {
                    LOG.info("Invalid line: '" + StringEscapeUtils.escapeJava(line) + "'");
                }
            }

            if (packedDest.size() > 0) {
                SparseMatrixRow row = new SparseMatrixRow(
                        vconf,
                        lastSrc.toInt(),
                        packedDest.toArray(),
                        new short[packedDest.size()]
                );
                writer.writeRow(row);
            }
            LOG.info("finalizing adjacency matrix");
            writer.finish();

            LOG.info("loading adjacency matrix");
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

    public static class Provider extends org.wikibrain.conf.Provider<LocalLinkDao> {
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
