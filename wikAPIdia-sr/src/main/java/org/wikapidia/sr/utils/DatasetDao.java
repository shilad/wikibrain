package org.wikapidia.sr.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class DatasetDao {

    public DatasetDao() {
    }

    public Dataset read(Language language, String path) throws DaoException {
        List<KnownSim> result = new ArrayList<KnownSim>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String delim = "\t";
            if (path.toLowerCase().endsWith("csv")) {
                delim = ",";
            }
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                String tokens[] = line.split(delim);
                if (tokens.length == 3) {
                    result.add(new KnownSim(
                            tokens[0],
                            tokens[1],
                            Double.valueOf(tokens[2]),
                            language
                    ));
                } else {
                    throw new DaoException("Invalid line in dataset file " + path+ ": " +
                            "'" + StringEscapeUtils.escapeJava(line) + "'");
                }
            }

        } catch (IOException e) {
            throw new DaoException(e);
        }
        return new Dataset(language, result);
    }

    public void write(Dataset dataset, String path) throws DaoException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            String delim = "\t";
            for (KnownSim ks: dataset.data) {
                writer.write(ks.phrase1 + delim + ks.phrase2 + delim + ks.similarity + "\n");
            }
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }
}
