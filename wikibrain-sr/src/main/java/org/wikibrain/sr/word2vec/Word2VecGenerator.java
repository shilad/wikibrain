package org.wikibrain.sr.word2vec;

import com.typesafe.config.Config;
import gnu.trove.list.TCharList;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.commons.io.IOUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.vector.VectorGenerator;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads in a word2vec model in the "standard" file format.
 * This code is adapted from https://github.com/ansjsun/Word2VEC_java
 *
 * @author Shilad Sen
 */
public class Word2VecGenerator implements VectorGenerator {
    private static final Logger LOG = Logger.getLogger(Word2VecGenerator.class.getName());

    private final Language language;
    private final LocalPageDao localPageDao;

    private Map<String, float[]> wordVectors = new HashMap<String, float[]>();
    private Map<Integer, float[]> articleVectors = new HashMap<Integer, float[]>();

    public Word2VecGenerator(Language language, LocalPageDao localPageDao, File path) throws IOException {
        this.language = language;
        this.localPageDao = localPageDao;
        if (wordVectors.isEmpty()) {
            this.read(path);
        }
    }

    public void read(File path) throws IOException {
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

            int numWords = Integer.parseInt(tokens[0]);
            int vlength = Integer.parseInt(tokens[1]);
            LOG.info("preparing to read " + numWords + " with length " + vlength + " vectors");

            for (int i = 0; i < numWords; i++) {
                String word = readString(dis);
                if (i % 50000 == 0) {
                    LOG.info("Read word vector " + word + " (" + i + " of " + numWords + ")");
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
                    articleVectors.put(wpId, vector);
                } else {
                    wordVectors.put(normalize(word), vector);
                }
            }
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(dis);
        }
    }


    private static String readString(DataInputStream dis) throws IOException {
        TCharList bytes = new TCharArrayList();
        while (true) {
            int i = dis.read();
            if (i < 0) {
                break;
            }
            char c = (char)i;
            if (c == ' ') {
                break;
            }
            if (c != '\n') {
                bytes.add(c);
            }
        }
        return new String(bytes.toArray());
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
    public TIntFloatMap getVector(int pageId) throws DaoException {
        throw new UnsupportedOperationException("Word2Vec only supports phrases, not pages.");
    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        float[] vector = wordVectors.get(normalize(phrase));
        if (vector == null) {
            return null;
        }
        TIntFloatMap result = new TIntFloatHashMap();
        for (int i = 0; i < vector.length; i++) {
            result.put(i, vector[i]);
        }
        return result;
    }

    @Override
    public List<Explanation> getExplanations(LocalPage page1, LocalPage page2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        return null;
    }

    private static  String normalize(String s) {
        return s.replace('_', ' ').trim();
    }


    public static class Provider extends org.wikibrain.conf.Provider<VectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<VectorGenerator> getType() {
            return VectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.generator";
        }

        @Override
        public VectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("word2vec")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            File path = new File(config.getString("path"));
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
}
