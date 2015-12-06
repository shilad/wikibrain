package org.wikibrain.core.cmd;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.lang.LanguageSet;

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
 * Three methods that can be used to set the LanguageSet:
 *      env.setUseLoaded() (default) uses the languages that have local pages loaded in the db
 *      env.setUseDownloaded() uses the languages that have downloaded article dumps
 *      env.setLanguageSet() explicitly specifies the language set.
 *
 * All three can also be passed via the command line (-l loaded, -l downloaded, or -l en,simple,fr).
 *
 * @author Shilad Sen
 */
public class EnvBuilder {
    private final Map<String, Object> params = new HashMap<String, Object>();
    File configOverride = null;

    public EnvBuilder() {
        setUseLoadedLanguages();
    }

    public EnvBuilder(CommandLine cmd) {
        initializeFromCommandLine(cmd);
    }

    private void initializeFromCommandLine(CommandLine cmd) {
        setUseLoadedLanguages();
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
            setConfigFile(new File(cmd.getOptionValue("c").trim()));
        }
        if (cmd.hasOption("l")) {
            String val = cmd.getOptionValue("l");
            if (val.equals("loaded")) {
                setUseLoadedLanguages();
            } else if (val.equals("downloaded")) {
                setUseDownloadedLanguages();
            } else if (val.equals("all")) {
                setLanguages(LanguageSet.ALL);
            } else {  // TODO: handle world economies, etc.
                setLanguages(new LanguageSet(cmd.getOptionValue("l")));
            }
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

    public EnvBuilder setReconfigureLogging(boolean reconfigure) {
        params.put("reconfigureLogging", reconfigure);
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

    public EnvBuilder setUseLoadedLanguages() {
        params.put("languages.default", "loaded");
        return this;
    }

    public EnvBuilder setUseDownloadedLanguages() {
        params.put("languages.default", "downloaded");
        return this;
    }

    public EnvBuilder setLanguages(LanguageSet langs) {
        params.put("languages.default", "manual");
        params.put("languages.manual.langCodes", langs.getLangCodes());
        return this;
    }

    public boolean hasExplicitLanguageSet() {
        String name = (String) params.get("languages.default");
        return !name.equals("loaded") && !name.equals("downloaded");
    }

    public EnvBuilder setLanguages(String langCodes) {
        return setLanguages(new LanguageSet(langCodes));
    }

    public EnvBuilder setProperty(String name, Object value) {
        params.put(name, value);
        return this;
    }

    /**
     * Adds the standard command line options to an options argument.
     */
    public static void addStandardOptions(Options options) {
        Option toAdd[] = new Option[] {
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
                        .withDescription("the set of languages to process, separated by commas or 'LOADED'")
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

    public static EnvBuilder builderFromArgs(String args[]) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String caller = stackTraceElements[1].getClassName();
        return builderFromArgs(caller, args);
    }

    public static EnvBuilder builderFromArgs(String caller, String args[]) {
        Options options = new Options();
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp(caller, options);
            return null;
        }
        return new EnvBuilder(cmd);
    }

    public static Env envFromArgs(String args[]) throws ConfigurationException {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String caller = stackTraceElements[1].getClassName();
        EnvBuilder builder = builderFromArgs(caller, args);
        if (builder == null) {
            return null;
        } else {
            return builder.build();
        }
    }
}
