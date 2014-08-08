package org.wikibrain.sr.factorization;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Translates a mostSimilar() sparse matrix to and from vowpal wabbit format.
 * Vowpal wabbit is used in lda mode to reduce the rank of the matrix.
 * Also see the bin/vowpal.sh script to run this.
 *
 * The result of the entire process is a dense matrix with rows for each article
 * and columns for each latent dimension.
 *
 * Also writes data files that can be used to interpret the results; both the lda topics
 * and the latent dimensions for each wikipedia page.
 *
 * This is particularly important because vowpal wabbit expects dense ids, but wp ids are sparse.
 *
 * The encoding step takes a sparse mostSimilar() matrix and and index helper (for wp titles) and
 * generates the following data files:
 *
 * - input.vw: Input file for vowpal.
 * - row_ids.tsv: Dense row id mapping file (one entry per WP article).
 * - col_ids.tsv: Dense column id mapping file (one entry per WP article that appears
 *   in a mostSimilar() result.
 *
 * Row / col id files have columns: dense id, wp id, wp article title.
 *
 * After Vowpal wabbit runs, it adds the following files:
 * - cache.vw: Cache file used internally by VW, not important.
 * - articles.vw: Mapping between articles and latent topics.
 * - topics.vw: Mapping between latent topics and WP articles.
 *
 * The decoding step takes the input directory and adds:
 * - topics.txt: Human-interpretable summary of topics.
 * - articles.matrix: Dense matrix between wp ids and latent topics.
 */
public class VowpalTranslator {
    private static final Logger LOG = Logger.getLogger(VowpalTranslator.class.getName());
    private final LocalPageDao pageDao;
    private final Language language;

    public VowpalTranslator(Language language, LocalPageDao pageDao) {
        this.language = language;
        this.pageDao = pageDao;
    }

    /**
     * Encode data into vowpal format.
     * Any existing data in the directory is deleted.
     *
     * @param matrix Matrix containing mostSimilar() results.
     * @param vowpalDir Output directory
     *
     * @throws IOException
     */
    public void encode(SparseMatrix matrix, File vowpalDir) throws IOException {
        FileUtils.deleteQuietly(vowpalDir);
        vowpalDir.mkdirs();

        encodeRows(new File(vowpalDir, "row_ids.tsv"), matrix);
        TIntIntMap colIdToDenseId = encodeExamples(new File(vowpalDir, "input.vw"), matrix);
        encodeCols(new File(vowpalDir, "col_ids.tsv"), colIdToDenseId);
    }

    private void encodeCols(File path, TIntIntMap colIdToDenseId) throws IOException {
        LOG.info("writing col_ids file");
        BufferedWriter wpWriter = new BufferedWriter(new FileWriter(path));
        for (int wpId : colIdToDenseId.keys()) {
            wpWriter.write(
                    "" + colIdToDenseId.get(wpId) +
                    "\t" + wpId +
                    "\t" + getTitle(wpId) + "\n"
            );
        }
        wpWriter.close();
    }

    private TIntIntMap encodeExamples(File path, SparseMatrix matrix) throws IOException {
        LOG.info("writing vw input file");
        BufferedWriter vwWriter = new BufferedWriter(new FileWriter(path));
        DecimalFormat df = new DecimalFormat("#.######");
        TIntIntMap colIdToDenseId = new TIntIntHashMap();
        for (SparseMatrixRow row : matrix) {
            vwWriter.write("|");
            for (int i = 0; i < row.getNumCols(); i++) {
                int colId = row.getColIndex(i);
                int denseId = colIdToDenseId.adjustOrPutValue(colId, 0, colIdToDenseId.size());
                vwWriter.write(" " + denseId + ":" + df.format(row.getColValue(i)));
            }
            vwWriter.write("\n");
        }
        vwWriter.close();
        return colIdToDenseId;
    }

    private void encodeRows(File path, SparseMatrix matrix) throws IOException {
        LOG.info("writing row_ids file");
        BufferedWriter rowWriter = new BufferedWriter(new FileWriter(path));
        int rowIds[] = matrix.getRowIds();
        for (int i = 0; i < rowIds.length; i++) {
            rowWriter.write(
                    "" + i +
                    "\t" + rowIds[i] +
                    "\t" + getTitle(rowIds[i]) + "\n"
            );
        }
        rowWriter.close();
    }

    private String getTitle(int id) {
        try {
            return pageDao.getById(language, id).getTitle().getCanonicalTitle();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Decodes the results of vowpal into a format interpretable by humans and this framework.
     *
     * @param dir Directory containing all data files.
     * @throws IOException
     */
    public void decode(File dir) throws IOException {
        WpEntry [] cols = readIdMapping(new File(dir, "col_ids.tsv"));
        decodeTopics(
                new File(dir, "topics.vw"),
                new File(dir, "topics.txt"), cols);
        WpEntry [] rows = readIdMapping(new File(dir, "row_ids.tsv"));
        decodeArticles(
                new File(dir, "articles.vw"),
                new File(dir, "articles.matrix"),
                rows
            );
    }

    private void decodeTopics(File pathVw, File pathOut, WpEntry[] cols) throws IOException {
        LOG.info("Decoding topics from " + pathVw + " to " + pathOut);
        BufferedReader reader = new BufferedReader(new FileReader(pathVw));

        // read header, including number of topics
        int numTopics = -1;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("reached eof while reading header of " + pathVw);
            }
            if (line.startsWith("lda:")) {
                numTopics = Integer.valueOf(line.substring(4).trim());
            }
            if (line.startsWith("options:")) {
                break;
            }
        }

        // create a leaderboard of most important articles for each latent topic
        Leaderboard top[] = new Leaderboard[numTopics];
        for (int i = 0; i < top.length; i++) { top[i] = new Leaderboard(100); }

        // read rows. each row represents an article and its distribution in latent topic space.
        for (int i = 0; i < cols.length; i++) {
            if (i % 100000 == 0) {
                LOG.info("decoding topics for column " + i + " of " + cols.length);
            }
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("reached eof while reading column " + i + " of " + cols.length);
            }
            String tokens[] = line.trim().split("\\s+");
            if (tokens.length != numTopics+1) {
                LOG.warning(
                        "invalid line for entry " + i +
                        ", expected " + numTopics + " topics, found " + (tokens.length-1) +
                        ": " + StringEscapeUtils.escapeJava(line)
                );
                continue;
            }
            int denseId = Integer.valueOf(tokens[0]);
            for (int t = 0; t < numTopics; t++) {
                top[t].tallyScore(denseId, Double.valueOf(tokens[t + 1]));
            }
        }
        reader.close();
        LOG.info("finished decoding topics for " + cols.length + " columns");

        // Write human-interpretable description
        BufferedWriter writer = new BufferedWriter(new FileWriter(pathOut));
        writer.write("Describing model in " + pathVw + " with " + numTopics + " latent topics.\n\n");
        DecimalFormat df = new DecimalFormat("#.##");
        for (int t = 0; t < numTopics; t++) {
            writer.write("\nTopic " + t + ":\n");
            SRResultList dsl = top[t].getTop();
            dsl.sortDescending();
            for (int i = 0; i < dsl.numDocs(); i++) {
                WpEntry c = cols[dsl.getId(i)];
                writer.write(
                        "\t" + (i+1) + ". " +
                        df.format(dsl.getScore(i)) + ", " +
                        c.wpId + ": " + c.title);
            }
        }
        writer.close();
    }

    /**
     * Reads the mapping from dense vowpal ids to sparse wp ids with filenames.
     * An entry at index i in the returned list will have dense id i.
     *
     * @param path
     * @return
     * @throws IOException
     */
    private WpEntry[] readIdMapping(File path) throws IOException {
        LOG.info("reading id mapping in " + path);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        List<WpEntry> entries = new ArrayList<WpEntry>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split("\t", 3);
            if (tokens.length != 3) {
                LOG.warning("invalid line in " + path + ": " +
                            StringEscapeUtils.escapeJava(line));
                continue;
            }
            WpEntry entry = new WpEntry();
            entry.denseId = Integer.valueOf(tokens[0]);
            entry.wpId = Integer.valueOf(tokens[1]);
            entry.title = tokens[2];
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            return new WpEntry[0];
        }

        // sort by dense id
        Collections.sort(entries, new Comparator<WpEntry>() {
            @Override
            public int compare(WpEntry wp1, WpEntry wp2) {
                return wp1.denseId - wp2.denseId;
            }
        });

        // make dense array
        int maxId = entries.get(entries.size() - 1).denseId;
        WpEntry[] dense = new WpEntry[maxId + 1];
        for (WpEntry e : entries) {
            dense[e.denseId] = e;
        }
        LOG.info("finished reading " + dense.length + " entries in mapping");

        return dense;
    }

    private void decodeArticles(File vowpalPreds, File destMatrix, WpEntry[] rows) throws IOException {
        LOG.info("decoding articles from " + vowpalPreds + " to " + destMatrix);
        BufferedReader reader = new BufferedReader(new FileReader(vowpalPreds));
        ValueConf vconf = new ValueConf(0.0f, 1.0f);
        DenseMatrixWriter writer = new DenseMatrixWriter(destMatrix, vconf);

        int colIds[] = null;
        for (int denseId = 0; denseId < rows.length; denseId++) {
            if (denseId % 100000 == 0) {
                LOG.info("decoding article " + denseId + " of " + rows.length);
            }       
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("reached eof while reading row " + denseId + " of " + rows.length);
            }
            String tokens[] = line.trim().split("\\s+");
            if (colIds == null) {
                colIds = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) colIds[i] = i;
            }
            if (tokens.length != colIds.length) {
                throw new IllegalStateException(
                        "expected " + colIds.length +
                                " tokens, found " + tokens.length +
                                " in line " + StringEscapeUtils.escapeJava(line));
            }
            float nums[] = new float[tokens.length];
            double sum = 0.0;
            for (int i = 0; i < tokens.length; i++) {
                nums[i] = Float.valueOf(tokens[i]);
                sum += nums[i];
            }
            if (sum != 0) {
                for (int i = 0; i < nums.length; i++) {
                    nums[i] /= sum;
                }
            }
            int rowId = rows[denseId].wpId;
            writer.writeRow(new DenseMatrixRow(vconf, rowId, colIds, nums));
        }
        reader.close();
        writer.finish();
        LOG.info("finishinged decoding " + rows.length + " articles");
    }

    public static class WpEntry {
        int denseId;
        int wpId;
        String title;
    }

    public void runVowpal(File dir, int dimensions) throws IOException {
        String d = dir.getAbsolutePath();
        CommandLine cmdLine = new CommandLine("vw");
        FileUtils.touch(new File(d, "cache.vw"));
        cmdLine.addArguments(new String[]{
                "--lda", "" + dimensions,
//                "--lda_alpha", "0.1",
//                "--lda_rho", "0.1",
                "--lda_D", "120000",
                "--minibatch", "100000",
//                "--power_t", "0.5",
//                "--initial_t", "1",
                "--passes", "2",
                "--cache_file", d + "/cache.vw",
                "-d", d + "/input.vw",
                "-p", d + "/articles.vw",
                "--readable_model", d + "/topics.vw",
                "-b", "19"
        });

        DefaultExecutor executor = new DefaultExecutor();
        int exitValue = executor.execute(cmdLine);
    }

    public static void usage() {
        System.err.println("usage: java " + VowpalTranslator.class.getName() + "\n" +
                "\t\tencode conf_file sparse_matrix vw_output_dir\n" +
                "\t\tdecode vw_dir\n"
        );
        System.exit(1);
    }

    public static void main(String args[]) throws IOException, ConfigurationException {
        if (args.length < 1) {
            usage();
        }
        Env env = EnvBuilder.envFromArgs(args);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        Language language = env.getLanguages().getDefaultLanguage();

        VowpalTranslator t = new VowpalTranslator(language, pageDao);

        File mf = new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/inlink/simple/feature.matrix");
        File f = new File("dat/sr/topics/simple/");
        SparseMatrix m = new SparseMatrix(mf);
        t.encode(m, f);
        t.runVowpal(f, 50);
        t.decode(f);

        /*if (args[0].equals("encode")) {
            if (args.length != 4) usage();
            EnvConfigurator env = new EnvConfigurator(
                    new ConfigurationFile(new File(args[1])));
            IndexHelper helper = env.loadIndex("main");
            t.encode(helper, new File(args[2]), new File(args[3]));
        } else if (args[0].equals("decode")) {
            if (args.length != 2) usage();
            t.decode(new File(args[1]));
        } else {
            usage();
        } */
    }
}