package org.wikibrain.wikidata;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.wikibrain.parser.DumpSplitter;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a test wikidata dump that extracts some records from a dump file:
 * - The first 400 items
 * - All properties
 *
 * @author Shilad Sen
 */
public class CreateTestDump {
    private static Logger LOG = LoggerFactory.getLogger(CreateTestDump.class);

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.err.println(
                    "Usage: java " + CreateTestDump.class +
                    " all_wikidata_input.bz2 test_extract_output.bz2\n");
            System.exit(1);
        }
        final AtomicInteger i = new AtomicInteger();
        final AtomicInteger articles = new AtomicInteger();
        final List<String> filtered = Collections.synchronizedList(new ArrayList<String>());
        BufferedReader reader = WpIOUtils.openBufferedReader(new File(args[0]));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.endsWith(",")) {
                line = line.substring(0, line.length()-1);
            }
            if (articles.incrementAndGet() % 100000 == 0) {
                LOG.info("processing entry " + articles);
            }
            if (line.contains("\"type\":\"property\"")) {
                filtered.add(line);
            } else if (line.contains("\"type\":\"item\"") && i.incrementAndGet() < 400) {
                filtered.add(line);
            }
        }
        final BufferedWriter writer = WpIOUtils.openBZ2Writer(new File(args[1]));
        writer.write("[\n");
        writer.write(StringUtils.join(filtered, ",\n"));
        writer.write("\n]\n");
        writer.close();
    }
}
