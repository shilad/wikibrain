package org.wikibrain.sr.pairwise;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.DenseMatrixWriter;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.wikibrain.utils.WbMathUtils.double2Float;

/**
 * @author Shilad Sen
 */
public class SciKitLearnDictionary {
    private static final Logger LOG = Logger.getLogger(SciKitLearnDictionary.class.getName());

    private File matrixPath = new File("cosimilarity.dat");
    private File rowPath = new File("cosimilarity_ids.txt");

    private final Random random = new Random();
    private final Env env;
    private final Language language;
    private final LocalPageDao pageDao;
    private final SRMetric metric;

    private int nextIndex = 0;
    private TreeMap<Integer, float[]> rowBuffer = new TreeMap<Integer, float[]>();

    private final int candidates[];

    public SciKitLearnDictionary(Env env, SRMetric metric, TIntSet candidateSet) throws ConfigurationException, IOException {
        this.env = env;
        this.metric = metric;
        this.language = metric.getLanguage();
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.candidates = candidateSet.toArray();
        Arrays.sort(candidates);
        writeCosimilarityMatrix();
    }

    private void writeCosimilarityMatrix() throws IOException {
        BufferedWriter writer = WpIOUtils.openWriter(rowPath);
        for (int i = 0 ; i < candidates.length; i++) {
            writer.write(i + "\t" + candidates[i] + "\t");
            try {
                LocalPage page = pageDao.getById(language, candidates[i]);
                writer.write(page.getTitle().getCanonicalTitle());
                writer.write("\n");
            } catch (DaoException e) {
                throw new IOException(e);
            }
        }
        writer.close();

        final TIntSet candidateSet = new TIntHashSet(candidates);

        //Mapping a file into memory
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(matrixPath));
        final float defaultValues[] = new float[candidates.length];
        Arrays.fill(defaultValues, (float) Math.sqrt(1.0 / candidates.length));

        ParallelForEach.range(0, candidates.length, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                try {
                    writeCosimilarities(out, candidateSet, i);
                } catch (Exception e) {
                    writeRow(out, i, defaultValues);
                }
            }
        });

        out.close();
    }

    private void writeCosimilarities(BufferedOutputStream out, TIntSet candidateSet, int i) throws DaoException, IOException {
        SRResultList mostSimilar = metric.mostSimilar(candidates[i], candidates.length / 10, candidateSet);
        mostSimilar.sortDescending();
        if (i % 1000 == 0) {
            LOG.info("building cosimilarity for " + i + " out of " + candidates.length + " with id " + candidates[i]
                            + " with min score " + mostSimilar.minScore()
            );
        }
        float scores[] = new float[candidates.length];
        Arrays.fill(scores, (float) (mostSimilar.minScore() * 0.7));
        mostSimilar.sortById();
        int k = 0;
        for (int j = 0; j < mostSimilar.numDocs(); j++) {
            int id = mostSimilar.getId(j);
            while (candidates[k] < id) { k++; }
            if (candidates[k] != id) {
                throw new IllegalStateException();
            }
            scores[k] = (float) mostSimilar.getScore(j);
        }

        writeRow(out, i, scores);
    }

    private synchronized void writeRow(BufferedOutputStream out, int index, float values[]) throws IOException {
        rowBuffer.put(index, values);
        while (rowBuffer.containsKey(nextIndex)) {
            for (int i = 0; i < values.length; i++) {
                int bits = Float.floatToIntBits(values[i]);
                byte[] bytes = new byte[4];
                bytes[0] = (byte)(bits & 0xff);
                bytes[1] = (byte)((bits >> 8) & 0xff);
                bytes[2] = (byte)((bits >> 16) & 0xff);
                bytes[3] = (byte)((bits >> 24) & 0xff);
                out.write(bytes);
            }
            nextIndex++;
        }
    }

    public static void main(String args[]) throws ConfigurationException, IOException {

        Env env = EnvBuilder.envFromArgs(args);
        Language language = env.getLanguages().getDefaultLanguage();
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "milnewitten", "language", language.getLangCode());

        TIntSet concepts = new TIntHashSet();
        String conceptPath = env.getConfiguration().getString("sr.concepts.path")
                + "/" + language.getLangCode() + ".txt";
        for (String line : FileUtils.readLines(new File(conceptPath))) {
            concepts.add(Integer.valueOf(line.trim()));
        }

        SciKitLearnDictionary skd = new SciKitLearnDictionary(env, metric, concepts);

    }

}
