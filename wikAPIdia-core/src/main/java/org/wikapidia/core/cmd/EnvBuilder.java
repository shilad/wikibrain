package org.wikapidia.core.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds an environment by setting common options.
 *
 * Example usage:
 *
 * Env env = new EnvBuilder(cmd)
 *                  .setProperty("phrases.loading", true)
 *                  .setLanguageSet(new LanguageSet(Arrays.asList("en", "it")))
 *                  .build();
 *
 * @author Shilad Sen
 */
public class EnvBuilder {
    private final Map<String, Object> params = new HashMap<String, Object>();
    File configOverride = null;

    public EnvBuilder() {}

    public EnvBuilder(CommandLine cmd) {
        if (cmd.hasOption("n")) {
            setConceptMapper(cmd.getOptionValue("n"));
        }
        if (cmd.hasOption("base-dir")) {
            setBaseDir(new File(cmd.getOptionValue("base-dir")));
        }
        if (cmd.hasOption("h")) {
            setMaxThreads(Integer.valueOf(cmd.getOptionValue("h")));
        }
        if (cmd.hasOption("c")) {
            setConfigFile(new File(cmd.getOptionValue("c")));
        }
        if (cmd.hasOption("l")) {
            setLanguages(new LanguageSet(cmd.getOptionValue("l")));
        }
        if (cmd.hasOption("tmp-dir")) {
            setTmpDir(new File(cmd.getOptionValue("tmp-dir")));
        }
    }

    public EnvBuilder setConceptMapper(String name) {
        params.put("mapper.default", name);
        return this;
    }

    public EnvBuilder setBaseDir(String dir) {
        return setBaseDir(new File(dir));
    }

    public EnvBuilder setBaseDir(File dir) {
        params.put("baseDir", dir.getAbsolutePath());
        return this;
    }

    public EnvBuilder setMaxThreads(int threads) {
        params.put("maxThreads", threads);
        return this;
    }

    public EnvBuilder setTmpDir(String dir) {
        return setTmpDir(new File(dir));
    }

    public EnvBuilder setTmpDir(File dir) {
        params.put("tmpDir", dir.getAbsolutePath());
        return this;
    }

    public EnvBuilder setConfigFile(String file) {
        return setConfigFile(new File(file));
    }

    public EnvBuilder setConfigFile(File file) {
        this.configOverride = file;
        return this;
    }

    public EnvBuilder setLanguages(LanguageSet langs) {
        params.put("languages.custom.langCodes", langs.getLangCodes());
        params.put("languages.default", "custom");
        return this;
    }

    public EnvBuilder setProperty(String name, Object value) {
        params.put(name, value);
        return this;
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

    public Env build() throws ConfigurationException {
        if (configOverride == null) {
            return new Env(params);
        } else {
            return new Env(params, configOverride);
        }
    }
}
