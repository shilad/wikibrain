package org.wikibrain.dao.load;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.apache.commons.cli.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.MetaInfo;
import org.wikibrain.utils.WbCommandLine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * Runs stages in the pipeline.
 * The stages are specified in the reference.conf and can be turned on or off using command line params.
 *
 * @author Shilad Sen
 */
public class PipelineLoader {
    public static final String DEFAULT_GROUP = "core";
    public static final String MULTILINGUAL_GROUP = "multilingual-core";

    public static Logger LOG = java.util.logging.Logger.getLogger(PipelineLoader.class.getName());

    private final Map<String, MetaInfo> state;
    private final LanguageSet langs;
    private final LinkedHashMap<String, PipelineStage> stages = new LinkedHashMap<String, PipelineStage>();
    private final Map<String, List<String>> groups = new HashMap<String, List<String>>();
    private boolean forceRerun = false;

    public PipelineLoader(Env env) throws ConfigurationException, DaoException, ClassNotFoundException, InterruptedException {
        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        this.langs = env.getLanguages();
        this.state = metaDao.getAllCummulativeInfo();
        initConfig(env.getConfiguration());
    }


    public void run(String [] args) throws IOException, InterruptedException {
        LOG.info("Beginning loading");
        for (PipelineStage stage : stages.values()) {
            if (stage.getShouldRun() != null && stage.getShouldRun()) {
                LOG.info("Beginning stage " + stage.getName());
                stage.runWithDependenciesIfNeeded(args, forceRerun);
                LOG.info("Successfully completed stage " + stage.getName());
            }
        }
        LOG.info("Loading successfully finished");
    }

    public void initConfig(Configuration config) throws ClassNotFoundException {
        for (Config stageConfig : config.get().getConfigList("loader.stages")) {
            PipelineStage stage = new PipelineStage(stageConfig, stages.values(), state);
            stages.put(stage.getName(), stage);
        }

        // Set up the groups
        Config groupConfig = config.get().getConfig("loader.groups");
        for (String g : config.get().getObject("loader.groups").keySet()) {
            groups.put(g, new ArrayList<String>());
            for (String s : groupConfig.getStringList(g)) {
                PipelineStage stage = getStage(s);  // throws IllegalArgumentException if unknown stage
                groups.get(g).add(s);
            }
        }
    }

    public void setStageOptions(String [] stageOpts) {
        // If no options are specified, use defaults
        if (stageOpts == null || stageOpts.length == 0) {
            if (langs.size() <= 1) {
                stageOpts = new String[] { DEFAULT_GROUP };
            } else {
                stageOpts = new String[] { MULTILINGUAL_GROUP };
            }
        }

        // expand groups in the options to the individual stages
        List<String> expanded = new ArrayList<String>();
        for (String opts : stageOpts) {
            String tokens[] =  opts.split(":", 2);
            if (groups.containsKey(tokens[0])) {
                for (String s : groups.get(tokens[0])) {
                    if (tokens.length == 1) {
                        expanded.add(s);
                    } else {
                        expanded.add(s + ":" + tokens[1]);
                    }
                }
            } else {
                expanded.add(opts);
            }
        }

        // Run with the requested options
        for (String opts : expanded) {
            String tokens[] =  opts.split(":", 3);  // "stage:{on|off}:args

            PipelineStage stage = getStage(tokens[0]);
            boolean shouldRunOverride = true;
            if (tokens.length >= 2) {
                if (!Arrays.asList("on", "off").contains(tokens[1])) {
                    throw new IllegalArgumentException("arg format for -s is stagename:{on|off}[:args]");
                }
                shouldRunOverride = tokens[1].equals("on");
            }

            String [] argsOverride = null;
            if (tokens.length == 3) {
                argsOverride = WbCommandLine.translateCommandline(tokens[2]);
            }
            stage.setOverrideOptions(shouldRunOverride, argsOverride);
        }
    }

    private PipelineStage getStage(String name) {
        PipelineStage stage = stages.get(name);
        if (stage == null) {
            throw new IllegalArgumentException("Unknown stage: " + name);
        }
        return stage;
    }


    public void setForceRerun(boolean forceRerun) {
        this.forceRerun = forceRerun;
    }

    public static void main(String args[]) throws ConfigurationException, ClassNotFoundException, IOException, InterruptedException, SQLException, DaoException {
        Options options = new Options();

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("stage")
                        .withDescription("turn stage on or off, format is stagename:on or stagename:off")
                        .create("s"));

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("groups")
                        .withDescription("groups that should be run (default is " + DEFAULT_GROUP + ")")
                        .create("g"));

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop")
                        .withDescription("Rerun all stages (e.g. drop previous data) whether or not stages have previously been run")
                        .create("d"));


        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("PipelineLoader", options);
            System.exit(1);
            return;
        }

        List<String> keeperArgs = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d") || args[i].equals("-drop")) {
                    // do not keep
            } else if (args[i].equals("-s") || args[i].equals("-stage")) {
                i++;    // do not keep and skip the next arg
            } else {
                keeperArgs.add(args[i]);
            }
        }
        LOG.info("pipeline keeping args: " + keeperArgs);


        Env env = new EnvBuilder(cmd).build();
        try {
            PipelineLoader loader = new PipelineLoader(env);

            // Close and pause
            env.close();
            Thread.sleep(1000);

            loader.setStageOptions(cmd.getOptionValues("s"));
            if (cmd.hasOption("d")) {
                loader.setForceRerun(true);
            }
            loader.run(keeperArgs.toArray(new String[0]));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Invalid arguments: " + e.getMessage());
            new HelpFormatter().printHelp("PipelineLoader", options);
            System.exit(1);
        }
    }
}
