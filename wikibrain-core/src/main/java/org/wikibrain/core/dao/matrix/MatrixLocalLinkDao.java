package org.wikibrain.core.dao.matrix;

import com.google.code.externalsorting.ExternalSort;
import com.typesafe.config.Config;
import gnu.trove.function.TDoubleFunction;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntProcedure;
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

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class wraps a local link dao delegate and builds a fast, sparse, matrix
 * and its transpose to speed up graph lookups.</p>
 *
 * <p>
 * Three API calls are partially supported:
 * 1. The three-argument version of getLinks()
 * 2. get() if a) a language and b) either a src or dest is specified.
 * 3. count() for the same requirements as 2.
 * 4. PageRank values (beware that PageRank estimates are lazily calculated
 * the first time a pagerank value is requested.)
 * </p>
 *
 * <p>
 * All other calls are delegated to the passed-in delegate.
 * Note that this dao also loads the links into the delegate.
 * </p>
 *
 * @author Shilad Sen
 */
public class MatrixLocalLinkDao implements LocalLinkDao {
    private static final Logger LOG = LoggerFactory.getLogger(MatrixLocalLinkDao.class);

    private final File dir;
    private LocalLinkDao delegate;
    private SparseMatrix matrix = null;
    private SparseMatrix transpose = null;
    private Map<Language, TIntDoubleMap> pageRanks = null;

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

        if (matrix == null && transpose == null) {
            boolean delegateHasData = false;
            try {
                delegateHasData = delegate.get(new DaoFilter().setLimit(1)).iterator().hasNext();
            } catch (Exception e) {
                LOG.warn("Error occurred while trying to fetch links from " + delegate +
                        ". Assuming it is empty and continuing.");
            }
            if (delegateHasData) {
                LOG.warn("MatrixLocalLinkDao empty, but delegate is not. Attempting to rebuild...");
                rebuild();
            }
        }
    }

    /**
     * Rebuild the dao from the delegate.
     * @throws DaoException
     */
    public void rebuild() throws DaoException {
        LocalLinkDao tmp = delegate;
        this.delegate = null;

        IOUtils.closeQuietly(matrix);
        IOUtils.closeQuietly(transpose);
        matrix = null;
        transpose = null;
        FileUtils.deleteQuietly(getMatrixFile());
        FileUtils.deleteQuietly(getTransposeFile());

        beginLoad();
        ParallelForEach.iterate(tmp.get(new DaoFilter()).iterator(), new Procedure<LocalLink>() {
            @Override
            public void call(LocalLink ll) throws Exception {
                save(ll);
            }
        });
        endLoad();

        this.delegate = tmp;
    }

    private void load() throws IOException {
        if (!getMatrixFile().isFile()) {
            LOG.warn("Matrix" + getMatrixFile()+ " missing, disabling fast lookups.");
        } else if (!getTransposeFile().isFile()) {
            LOG.warn("Matrix" + getTransposeFile()+ " missing, disabling fast lookups.");
        } else {
            matrix = new SparseMatrix(getMatrixFile());
            transpose = new SparseMatrix(getTransposeFile());
        }
        if (getPageRanksFile().isFile() && getPageRanksFile().lastModified() > getMatrixFile().lastModified()) {
            pageRanks = (Map<Language, TIntDoubleMap>) WpIOUtils.readObjectFromFile(getPageRanksFile());
        }
    }

    @Override
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException {
        return delegate.getLink(language, sourceId, destId);
    }

    @Override
    public void beginLoad() throws DaoException {
        allWriters.clear();
        if (delegate != null) delegate.beginLoad();
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

    /**
     * Calculates the PageRank associated with a particular page.
     * Currently only implemented by the MatrixLocalLinkDao.
     * PageRank estimation is performed lazily, so the first time this method is called
     * will be very expensive, and future invocations will be cached.
     *
     * @param language
     * @param pageId
     * @return An estimate of the pageRank. The sum of PageRank values for all pages will
     * approximately sum to 1.0.
     */
    @Override
    public double getPageRank(Language language, int pageId) {
        if (pageRanks == null) {
            synchronized (this) {
                if (pageRanks == null) {
                    pageRanks = computePageRanks();
                    try {
                        WpIOUtils.writeObjectToFile(getPageRanksFile(), pageRanks);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unexpected exception:", e);
                    }
                }
            }
        }
        TIntDoubleMap langRanks = pageRanks.get(language);
        if (langRanks != null && langRanks.containsKey(pageId)) {
            return langRanks.get(pageId);
        } else {
            return 0.0;
        }
    }

    /**
     * Calculates the PageRank associated with a particular page.
     * Currently only implemented by the MatrixLocalLinkDao.
     * PageRank estimation is performed lazily, so the first time this method is called
     * will be very expensive, and future invocations will be cached.
     *
     * @param localId
     * @return An estimate of the pageRank. The sum of PageRank values for all pages will
     * approximately sum to 1.0.
     */
    @Override
    public double getPageRank(LocalId localId) {
        return getPageRank(localId.getLanguage(), localId.getId());
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


    private static final double DAMPING_FACTOR = 0.85;


    private static class LangRanks {
        TIntDoubleMap pageSums = new TIntDoubleHashMap();
        TIntDoubleMap nextSums = new TIntDoubleHashMap();
    }
    private Map<Language, TIntDoubleMap> computePageRanks() {
        Map<Language, LangRanks> ranks = new HashMap<Language, LangRanks>();

        // Set initial weights
        for (SparseMatrixRow row : matrix) {
            LocalId src = LocalId.fromInt(row.getRowIndex());
            LangRanks lr = ranks.get(src.getLanguage());
            if (lr == null) {
                lr = new LangRanks();
                ranks.put(src.getLanguage(), lr);
            }
            lr.pageSums.put(src.getId(), 1.0);
        }

        // normalize (divide by num pages)
        for (final LangRanks lr : ranks.values()) {
            final int n = lr.pageSums.size();
            lr.pageSums.transformValues(new TDoubleFunction() {
                @Override
                public double execute(double v) {
                    return 1.0 / n;
                }
            });
        }

        // perform iterations
        for (int i = 0;i < 20; i++) {
            for (SparseMatrixRow row : matrix) {
                int ncols = row.getNumCols();
                if (ncols == 0) continue;
                LocalId src = LocalId.fromInt(row.getRowIndex());
                LangRanks lr = ranks.get(src.getLanguage());
                double w = lr.pageSums.get(src.getId()) / ncols;
                for (int j = 0; j < ncols; j++) {
                    LocalId dest = LocalId.fromInt(row.getColIndex(j));
                    if (dest.getLanguage() == src.getLanguage()) {
                        lr.nextSums.adjustOrPutValue(dest.getId(), w, w);
                    }
                }
            }

            // update values, measure change
            final double[] delta = {0.0};
            for (final LangRanks lr : ranks.values()) {
                final int n = lr.nextSums.size();
                lr.nextSums.forEachKey(new TIntProcedure() {
                    @Override
                    public boolean execute(int id) {
                        double ps = lr.pageSums.get(id);
                        double ns = (1.0 - DAMPING_FACTOR) / n + DAMPING_FACTOR * lr.nextSums.get(id);
                        delta[0] += Math.abs(ps - ns);
                        lr.nextSums.put(id, 0);
                        lr.pageSums.put(id, ns);
                        return true;

                    }
                });
            }
            LOG.info("change in pageranks at iteration {} is {}.", i, delta);
        }


        Map<Language, TIntDoubleMap> result = new HashMap<Language, TIntDoubleMap>();
        for (Language lang : ranks.keySet()) {
            result.put(lang, ranks.get(lang).pageSums);
        }
        return result;
    }

    @Override
    public void save(LocalLink item) throws DaoException {
        if (delegate != null) delegate.save(item);
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

    public File getPageRanksFile() {
        return new File(dir, "pageRanks.bin");
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
        if (delegate != null) delegate.endLoad();

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
