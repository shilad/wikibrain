package org.wikibrain.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.jooq.tables.LocalLink;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class MatrixSummarizer {
    private static final Logger LOG = LoggerFactory.getLogger(MatrixSummarizer.class);

    private final LocalPageDao pageDao;

    public MatrixSummarizer(LocalPageDao dao) {
        this.pageDao = dao;
    }

    public void summarize(Language language, File matrixFile) throws IOException, DaoException {
        SparseMatrix matrix = new SparseMatrix(matrixFile);
        summarize(language, matrix);
        matrix.close();
    }

    public void summarize(Language language, SparseMatrix matrix) throws DaoException {
        final TIntIntHashMap counts = new TIntIntHashMap();
        final TIntDoubleHashMap sums = new TIntDoubleHashMap();

        int rowNum = 0;
        for (SparseMatrixRow row : matrix) {
            for (int i = 0; i < row.getNumCols(); i++) {
                int id = row.getColIndex(i);
                double val = row.getColValue(i);
                counts.adjustOrPutValue(id, 1, 1);
                sums.adjustOrPutValue(id, val, val);
//                counts.adjustOrPutValue(row.getRowIndex(), 1, 1);
//                sums.adjustOrPutValue(row.getRowIndex(), val, val);
            }
            if (rowNum++ % 100000 == 0) {
                LOG.info("reading row " + rowNum + " of " + matrix.getNumRows() + "; unique col ids=" + counts.size());
            }
        }

        System.out.println("num rows: " + matrix.getNumRows());
        System.out.println("num columns: " + counts.size());

        // sort by counts
        Integer ids[] = ArrayUtils.toObject(counts.keys());
        Arrays.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                return counts.get(id2) - counts.get(id1);
            }
        });
        System.out.println("top counts:");
        for (int i = 0; i < Math.min(200, ids.length); i++) {
            System.out.println(
                    "" + (i+1) +
                    ". " + describePage(language, ids[i]) +
                    "(id=" + ids[i] + ")" +
                    " count = " + counts.get(ids[i]));
        }

        // sort by sums
        Arrays.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                double s1 = sums.get(id1);
                double s2 = sums.get(id2);
                if (s1 > s2) { return -1; }
                else if (s1 < s2) { return +1; }
                else { return 0; }
            }
        });
        System.out.println("top sums:");
        for (int i = 0; i < Math.min(200, ids.length); i++) {
            System.out.println(
                    "" + (i+1) +
                    ". " + describePage(language, ids[i]) +
                    "(id=" + ids[i] + ")" +
                    " sum = " + sums.get(ids[i]));
        }
    }

    private String describePage(Language language, Integer id) throws DaoException {
        LocalPage page = pageDao.getById(language, id);
        return (page == null) ? "unknown" : page.toString();
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("the name of the metrics whose matrices you want to summarize")
                        .create("m"));

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("types")
                        .withDescription("the types of matrices you want to summarize (feature, featureTranspose, or cosimilarity)")
                        .create("p"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MatrixSummarizer", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Language language = env.getLanguages().getDefaultLanguage();

        List<File> matrixPaths = new ArrayList<File>();
        if (!cmd.getArgList().isEmpty()) {
            for (Object arg : cmd.getArgList()) {
                matrixPaths.add(new File((String)arg));
            }
        }

        if (cmd.hasOption("m")) {
            File srDir = new File(env.getConfiguration().get().getString("sr.metric.path"));
            String name = cmd.getOptionValue("m");
            String types[] = new String[] {"feature", "cosimilarity" };
            if (cmd.hasOption("p")) {
                types = cmd.getOptionValues("p");
            }
            for (String t : types) {
                matrixPaths.add(FileUtils.getFile(srDir, name, language.getLangCode(), t + ".matrix"));
            }
        }

        MatrixSummarizer summarizer = new MatrixSummarizer(env.getConfigurator().get(LocalPageDao.class));
        for (File path : matrixPaths) {
            if (path.isFile()) {
                summarizer.summarize(language, path);
            } else {
                LOG.warn("skipping nonexistant matrix file " + path.getAbsolutePath());
            }
        }
    }
}
