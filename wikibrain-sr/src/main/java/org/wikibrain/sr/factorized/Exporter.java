package org.wikibrain.sr.factorized;

import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.InMemorySparseMatrix;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class Exporter {
    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        File dir = new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/");
        File input = FileUtils.getFile(dir, "symMostSimilar.matrix");
        File output = FileUtils.getFile(dir, "cosim");
        SparseMatrix m = new SparseMatrix(input);
        FactorizerUtils.writeTextFormat(m, output, env.getConfigurator().get(LocalPageDao.class), Language.SIMPLE);
    }
}
