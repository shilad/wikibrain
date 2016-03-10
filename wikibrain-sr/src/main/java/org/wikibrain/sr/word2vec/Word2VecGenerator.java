package org.wikibrain.sr.word2vec;

import com.typesafe.config.Config;
import gnu.trove.list.TByteList;
import gnu.trove.list.TCharList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.DenseMatrixWriter;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.vector.DenseVectorGenerator;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads in a word2vec model in the "standard" file format.
 *
 * Builds a disk
 *
 * This code is adapted from https://github.com/ansjsun/Word2VEC_java
 *
 * @author Shilad Sen
 */
public class Word2VecGenerator implements DenseVectorGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecGenerator.class);

    private final Language language;
    private final LocalPageDao localPageDao;
    private final File path;

    private TLongIntMap phraseIds;
    private DenseMatrix phraseMatrix;
    private DenseMatrix articleMatrix;

    public Word2VecGenerator(Language language, LocalPageDao localPageDao, File path) throws IOException {
        this.language = language;
        this.localPageDao = localPageDao;
        this.path = path;
        this.read();
    }

    public void read() throws IOException {
        if (getArticleMatrixPath().exists()
        &&  getPhraseMatrixPath().exists()
        &&  getPhraseIdPath().exists()
        &&  getPhraseMatrixPath().lastModified() >= path.lastModified()
        &&  getArticleMatrixPath().lastModified() >= path.lastModified()) {
            LOG.info("phrase and article caches are up to date, loading them...");
            phraseMatrix = new DenseMatrix(getPhraseMatrixPath());
            articleMatrix = new DenseMatrix(getArticleMatrixPath());
            readPhraseIds();
        } else {
            createWikiBrainModel();
        }
    }

    private void readPhraseIds() throws IOException {
        BufferedReader reader = WpIOUtils.openBufferedReader(getPhraseIdPath());
        try {
            phraseIds = new TLongIntHashMap();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String tokens[] = line.split("\t", 2);
                int wpId = Integer.parseInt(tokens[0]);
                String phrase = tokens[1].trim();
                phraseIds.put(hashWord(phrase), wpId);
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private void createWikiBrainModel() throws IOException {
        FileUtils.deleteQuietly(getPhraseIdPath());
        FileUtils.deleteQuietly(getPhraseMatrixPath());
        FileUtils.deleteQuietly(getArticleMatrixPath());

        ValueConf vconf = new ValueConf();
        BufferedWriter phraseIdWriter = WpIOUtils.openWriter(getPhraseIdPath());
        DenseMatrixWriter phraseWriter = new DenseMatrixWriter(getPhraseMatrixPath(), vconf);
        DenseMatrixWriter articleWriter = new DenseMatrixWriter(getArticleMatrixPath(), vconf);

        DataInputStream dis = null;
        InputStream bis = null;
        try {
            bis = WpIOUtils.openInputStream(path);
            dis = new DataInputStream(bis);

            String header = "";
            while (true) {
                char c = (char) dis.read();
                if (c == '\n') break;
                header += c;
            }

            String tokens[] = header.split(" ");

            int numEntities = Integer.parseInt(tokens[0]);
            int vlength = Integer.parseInt(tokens[1]);
            LOG.info("preparing to read " + numEntities + " with length " + vlength + " vectors");
            int [] colIds = new int[vlength];
            for (int i = 0; i < vlength; i++) { colIds[i] = i; }
            int numPhrases = 0;
            int numArticles = 0;

            for (int i = 0; i < numEntities; i++) {
                String word = readString(dis);
                if (i % 5000 == 0) {
                    LOG.info("Read word vector " + word + " (" + i + " of " + numEntities + ")");
                }

                float[] vector = new float[vlength];
                double norm2 = 0.0;
                for (int j = 0; j < vlength; j++) {
                    float val = readFloat(dis);
                    norm2 += val * val;
                    vector[j] = val;
                }
                norm2 = Math.sqrt(norm2);

                for (int j = 0; j < vlength; j++) {
                    vector[j] /= norm2;
                }
                if (word.startsWith("/w/")) {
                    String[] pieces = word.split("/", 5);
                    int wpId = Integer.valueOf(pieces[3]);
                    if (wpId >= 0) {
                        DenseMatrixRow row = new DenseMatrixRow(vconf, wpId, colIds, vector);
                        articleWriter.writeRow(row);
                        numArticles++;
                    }
                } else {
                    word = word.replace('\t', ' ').replace('\n', ' ');
                    DenseMatrixRow row = new DenseMatrixRow(vconf, numPhrases, colIds, vector);
                    phraseWriter.writeRow(row);
                    phraseIdWriter.write(numPhrases + "\t" + word + "\n");
                    numPhrases++;
                }
            }
            if (numPhrases == 0) {
                phraseWriter.writeRow(new DenseMatrixRow(vconf, 0, colIds, new float[vlength]));
            }
            if (numArticles == 0) {
                articleWriter.writeRow(new DenseMatrixRow(vconf, 0, colIds, new float[vlength]));
            }
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(dis);
        }

        IOUtils.closeQuietly(phraseIdWriter);
        phraseWriter.finish();
        articleWriter.finish();

        phraseMatrix = new DenseMatrix(getPhraseMatrixPath());
        articleMatrix = new DenseMatrix(getArticleMatrixPath());
        readPhraseIds();
    }

    private File getPhraseMatrixPath() {
        return new File(path.getAbsolutePath() + ".phrases.matrix");
    }

    private File getArticleMatrixPath() {
        return new File(path.getAbsolutePath() + ".articles.matrix");
    }


    private File getPhraseIdPath() {
        return new File(path.getAbsolutePath() + ".phrases.txt");
    }



    private static String readString(DataInputStream dis) throws IOException {
        TByteList bytes = new TByteArrayList();
        while (true) {
            int i = dis.read();
            if (i == -1) {
                break;
            }
            if (i < 0 || i > 255) {
                throw new IllegalStateException();
            }
            char c = (char)i;
            if (c == ' ') {
                break;
            }
            if (c != '\n') {
                bytes.add((byte)i);
            }
        }
        return new String(bytes.toArray(), "UTF-8");
    }

    private static float readFloat(InputStream is) throws IOException {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }

    private static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }


    @Override
    public DenseMatrix getFeatureMatrix() {
        return articleMatrix;
    }

    @Override
    public float []  getVector(int pageId) throws DaoException {
        try {
            DenseMatrixRow row = articleMatrix.getRow(pageId);
            return row == null ? null : row.getValues();
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public float [] getVector(String phrase) {
        try {
            long hash = hashWord(phrase);
            if (phraseIds.containsKey(hash)) {
                int phraseId = phraseIds.get(hash);
                DenseMatrixRow row = phraseMatrix.getRow(phraseId);
                return row == null ? null : row.getValues();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static long hashWord(String word) {
        return Word2VecUtils.hashWord(normalize(word));
    }

    @Override
    public List<Explanation> getExplanations(String phrase1, String phrase2, float [] vector1, float [] vector2, SRResult result) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Explanation> getExplanations(int pageID1, int pageID2, float [] vector1, float [] vector2, SRResult result) throws DaoException {
        return null;
    }

    private static  String normalize(String s) {
        return s.replace('_', ' ').trim();
    }


    public static class Provider extends org.wikibrain.conf.Provider<DenseVectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<DenseVectorGenerator> getType() {
            return DenseVectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.densegenerator";
        }

        @Override
        public DenseVectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("word2vec")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            File path = getModelFile(config.getString("modelDir"), language);
            if (!path.isFile()) {
                throw new ConfigurationException("Path to word2vec model " + path.getAbsolutePath() + " is not a file. Do you need to download or build the model?");
            }
            try {
                return new Word2VecGenerator(
                        language,
                        getConfigurator().get(LocalPageDao.class),
                        path
                );
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    public static File getModelFile(String dir, Language lang) {
        return getModelFile(new File(dir), lang);
    }

    public static File getModelFile(File dir, Language lang) {
        return new File(dir, lang.getLangCode() + ".bin");
    }

    public static void main(String args[]) throws IOException {
        Word2VecGenerator gen = new Word2VecGenerator(null, null,
                new File("/Users/a558989/Projects/wikibrain/base-bh/dat/word2vecRaw/bh.bin"));
    }
}
