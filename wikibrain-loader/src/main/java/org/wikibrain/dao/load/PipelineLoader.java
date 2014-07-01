package org.wikibrain.dao.load;

import com.typesafe.config.Config;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
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

    public static Logger LOG = java.util.logging.Logger.getLogger(PipelineLoader.class.getName());

    private final EnvBuilder builder;
    private final Map<String, MetaInfo> state;
    private final LanguageSet langs;
    private final LinkedHashMap<String, PipelineStage> stages = new LinkedHashMap<String, PipelineStage>();
    private boolean forceRerun = false;

    public PipelineLoader(EnvBuilder builder) throws ConfigurationException, DaoException, ClassNotFoundException, InterruptedException {
        this.builder = builder;
        Env env = builder.build();
        try {
            MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
            this.langs = env.getLanguages();
            this.state = metaDao.getAllCummulativeInfo();
            initStages(env.getConfiguration());
        } finally {
            IOUtils.closeQuietly(env);
            Thread.sleep(1000);
        }
    }


    public void run(List<String> groups, String [] args) throws IOException, InterruptedException {
        validateGroups(groups);
        LOG.info("Beginning loading");
        for (PipelineStage stage : stages.values()) {
            if (stage.isNeededAtTopLevel(groups, forceRerun)) {
                LOG.info("Beginning stage " + stage.getName());
                stage.runWithDependencies(args);
                LOG.info("Successfully completed stage " + stage.getName());
            }
        }
        LOG.info("Loading successfully finished");
    }

    private void validateGroups(List<String> groups) {
        for (String g : groups) {
            boolean found = false;
            for (PipelineStage s : stages.values()) {
                if (g.equalsIgnoreCase(s.getGroup())) {
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown group requested: " + g);
            }
        }
    }

    public void initStages(Configuration config) throws ClassNotFoundException {
        for (Config stageConfig : config.get().getConfigList("loader.stages")) {
            PipelineStage stage = new PipelineStage(stageConfig, stages.values(), state);
            stages.put(stage.getName(), stage);
        }
    }

    public void setStageOptions(String [] stageOpts) {
        for (String opts : stageOpts) {
            String tokens[] =  opts.split(":", 3);
            if (tokens.length < 2 || !Arrays.asList("on", "off").contains(tokens[1])) {
                throw new IllegalArgumentException("arg format for -s is stagename:{on|off}[:args]");
            }
            PipelineStage stage = stages.get(tokens[0]);
            if (stage == null) {
                throw new IllegalArgumentException("Unknown stage: " + tokens[0]);
            }
            String [] argsOverride = null;
            boolean shouldRunOverride = tokens[1].equals("on");
            if (tokens.length == 3) {
                argsOverride = WbCommandLine.translateCommandline(tokens[2]);
            }
            stage.setOverrideOptions(shouldRunOverride, argsOverride);
        }
    }

    public void setForceRerun(boolean forceRerun) {
        this.forceRerun = forceRerun;
    }

    public static void main(String args[]) throws ConfigurationException, ClassNotFoundException, IOException, InterruptedException, SQLException, DaoException {
        Options options = new Options();

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("off")
                        .withDescription("turn all stages off by default (e.g. run no groups)")
                        .create("f"));

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
            if (args[i].equals("-f") || args[i].equals("-off")) {
                // do not keep
            } else if (args[i].equals("-d") || args[i].equals("-drop")) {
                    // do not keep
            } else if (args[i].equals("-s") || args[i].equals("-stage")) {
                i++;    // do not keep and skip the next arg
            } else if (args[i].equals("-g") || args[i].equals("-groups")) {
                i++;    // do not keep and skip the next arg
            } else {
                keeperArgs.add(args[i]);
            }
        }
        LOG.info("pipeline keeping args: " + keeperArgs);

        List<String> groups = new ArrayList<String>();
        if (cmd.hasOption("f")) {
            // groups is empty
        } else if (cmd.hasOption("g")) {
            groups.addAll(Arrays.asList(cmd.getOptionValues("g")));
        } else {
            groups.add(DEFAULT_GROUP);
        }

        EnvBuilder builder = new EnvBuilder(cmd);
        try {
            PipelineLoader loader = new PipelineLoader(builder);
            if (cmd.hasOption("s")) {
                loader.setStageOptions(cmd.getOptionValues("s"));
            }
            if (cmd.hasOption("d")) {
                loader.setForceRerun(true);
            }
            loader.run(groups, keeperArgs.toArray(new String[0]));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Invalid arguments: " + e.getMessage());
            new HelpFormatter().printHelp("PipelineLoader", options);
            System.exit(1);
        }
    }
}
