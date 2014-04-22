package org.wikapidia.wikidata;

import org.wikapidia.parser.DumpSplitter;
import org.wikapidia.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Creates a test wikidata dump that extracts some records from a dump file:
 * - The first 400 items
 * - All properties
 *
 * @author Shilad Sen
 */
public class CreateTestDump {
    private static Logger LOG = Logger.getLogger(CreateTestDump.class.getName());

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.err.println(
                    "Usage: java " + CreateTestDump.class.getName() +
                    " all_wikidata_input.bz2 test_extract_output.bz2\n");
            System.exit(1);
        }
        DumpSplitter splitter = new DumpSplitter(new File(args[0]));
        BufferedWriter writer = WpIOUtils.openBZ2Writer(new File(args[1]));
        writer.write("<mediawiki>\n");
        int i = 0;
        int articles = 0;
        for (String article : splitter) {
            if (articles++ % 100000 == 0) {
                LOG.info("processing article " + articles);
            }
            String strippedArticle = article.replaceAll("\\s+", "");
            if (strippedArticle.contains("<model>wikibase-item</model>")) {
                if (i++ < 400) {
                    writer.write(article + "\n");
                }
            } else if (strippedArticle.contains("<model>wikibase-property</model>")) {
                writer.write(article + "\n");
            }
        }
        writer.write("</mediawiki>\n");
        writer.close();
    }
}
