package org.wikibrain.dao.load;

import com.typesafe.config.Config;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.utils.JvmUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * Runs stages in the pipeline.
 * The stages are specified in the reference.conf and can be turned on or off using command line params.
 *
 * @author Shilad Sen
 */
public class PipelineLoader {
    public static Logger LOG =java.util.logging.Logger.getLogger(PipelineLoader.class.getName());

    /**
     * Whether the stage should be turned on by default.
     */
    static enum OnByDefault {
        TRUE,
        FALSE,
        IFMULTILINGUAL,           // only if more than one language is being used
    }

    static class Stage {
        OnByDefault onByDefault;
        String name;
        Class klass;
        String extraArgs[];

        Stage(Config config) throws ClassNotFoundException {
            this.name = config.getString("name");
            this.klass = Class.forName(config.getString("class"));
            this.onByDefault = OnByDefault.valueOf(config.getString("onByDefault").toUpperCase());
            this.extraArgs = config.getStringList("extraArgs").toArray(new String[0]);
        }
    }

    private final LanguageSet langs;
    private final String[] args;
    private final List<Stage> stages;

    public PipelineLoader(LanguageSet langs, List<Stage> stages, String args[]) {
        this.langs = langs;
        this.args = args;
        this.stages = stages;
    }

    public void run() throws IOException, InterruptedException {
        LOG.info("Beginning loading");
        for (Stage stage : stages) {
            LOG.info("Beginning stage " + stage.name);
            run(stage.klass, ArrayUtils.addAll(args, stage.extraArgs));
            LOG.info("Successfully completed stage " + stage.name);
        }
        LOG.info("Loading successfully finished");
    }

    public void run(Class klass, String args[]) throws IOException, InterruptedException {
        Process p = JvmUtils.launch(klass, args);
        int retVal = p.waitFor();
        if (retVal != 0) {
            System.err.println("command failed with exit code " + retVal + " : ");
            System.err.println("ABORTING!");
            System.exit(retVal);
        }
    }

    public static void main(String args[]) throws ConfigurationException, ClassNotFoundException, IOException, InterruptedException, SQLException {
        Options options = new Options();


        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("off")
                        .withDescription("turn all stages off by default")
                        .create("f"));

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("stage")
                        .withDescription("turn stage on or off, format is stagename:on or stagename:off")
                        .create("s"));

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
            } else if (args[i].equals("-s") || args[i].equals("-stage")) {
                i++;    // do not keep and skip the next arg
            } else {
                keeperArgs.add(args[i]);
            }
        }
        LOG.info("pipeline keeping args: " + keeperArgs);

        Env env = new EnvBuilder(cmd).build();
        Config config = env.getConfiguration().get();
        LanguageSet langs = env.getLanguages();

        // Shut down the database carefully
        WpDataSource ds = env.getConfigurator().get(WpDataSource.class);
        ds.close();
        Thread.sleep(1000);

        boolean offByDefault = cmd.hasOption("f");

        Map<String, Boolean> runStages = new HashMap<String, Boolean>();
        if (cmd.hasOption("s")) {
            for (String value : cmd.getOptionValues("s")) {
                String pair[] =  value.split(":");
                if (pair.length != 2 || (!pair[1].equals("on") && !pair[1].equals("off"))) {
                    System.err.println("arg format for -s is stagename:on or  stagename:off");
                    new HelpFormatter().printHelp("PipelineLoader", options);
                    System.exit(1);
                    return;
                }
                runStages.put(pair[0], pair[1].equals("on"));
            }
        }

        List<String> available = new ArrayList<String>();
        List<Stage> stages = new ArrayList<Stage>();
        for (Config stageConfig : config.getConfigList("loader.stages")) {
            Stage stage = new Stage(stageConfig);
            available.add(stage.name);

            boolean on = false;
            if (stage.onByDefault == OnByDefault.TRUE) {
                on = true;
            } else if (stage.onByDefault == OnByDefault.IFMULTILINGUAL && langs.size() > 1) {
                on = true;
            }

            if (runStages.containsKey(stage.name)) {
                on = runStages.get(stage.name);
                runStages.remove(stage.name);
            }

            if (on) {
                stages.add(stage);
            }
        }
        if (!runStages.isEmpty()) {
            System.err.println("Unknown stages: " + StringUtils.join(runStages.keySet(), ", "));
            System.err.println("Available stages are: " + StringUtils.join(available, ", "));
            new HelpFormatter().printHelp("PipelineLoader", options);
            System.exit(1);
            return;
        }
        PipelineLoader loader = new PipelineLoader(langs, stages, keeperArgs.toArray(new String[0]));
        loader.run();
    }
}
