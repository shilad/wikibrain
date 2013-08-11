package org.wikapidia.core.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.wikapidia.conf.DefaultOptionBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class EnvBuilder {
    private Map<String, Object> params = new HashMap<String, Object>();
    File configOverride = null;

    public EnvBuilder() {}

    public EnvBuilder(CommandLine cmd) {
        if (cmd.hasOption("n")) {
            params.put("mapper.default", cmd.getOptionValue("n"));
        }
        if (cmd.hasOption("base-dir")) {
            params.put("baseDir", cmd.getOptionValue("base-dir"));
        }
        if (cmd.hasOption("c")) {
            configOverride = new File(cmd.getOptionValue("c"));
        }
    }

    /**
     * Adds the standard command line options to an options argument.
     * @param options
     */
    public static void addStandardOptions(Options options) {
        Option toAdd[] = new Option[] {
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("algorithm name ")
                        .withDescription("universal concept map algorithm name")
                        .create("n"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("conf")
                        .withDescription("configuration file")
                        .create("c"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("threads")
                        .withDescription("the maximum number of threads that should be used")
                        .create("h"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process, separated by commas")
                        .create("l"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("base-dir")
                        .withDescription("the base directory used to resolve relative directories")
                        .create(),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("tmp-dir")
                        .withDescription("the temporary directory")
                        .create()
        };
        for (Option o : toAdd) {
            if (options.hasOption(o.getOpt())) {
                throw new IllegalArgumentException("Standard command line option " + o.getOpt() + " reused");
            }
            options.addOption(o);
        }
    }

    public Env build() {
        return null;
    }
}
