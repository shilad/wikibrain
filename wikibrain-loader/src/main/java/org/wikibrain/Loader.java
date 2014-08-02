package org.wikibrain;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.loader.pipeline.PipelineLoader;
import org.wikibrain.loader.pipeline.StageArgs;
import org.wikibrain.loader.pipeline.StageFailedException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class Loader {
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
                        .withLongOpt("drop")
                        .withDescription("Rerun all stages (e.g. drop previous data) whether or not stages have previously been run")
                        .create("d"));

        // TODO: If specified, don't append global arguments to stage arguments


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
        PipelineLoader.LOG.info("pipeline keeping args: " + keeperArgs);


        Env env = new EnvBuilder(cmd).build();
        try {
            List<StageArgs> stageArgs = null;
            if (cmd.hasOption("s")) {
                stageArgs = new ArrayList<StageArgs>();
                for (String s : cmd.getOptionValues("s")) {
                    stageArgs.add(new StageArgs(s));
                }
            }

            PipelineLoader loader = new PipelineLoader(env, stageArgs);

            // Close and pause
            env.close();
            Thread.sleep(1000);

            if (cmd.hasOption("d")) {
                loader.setForceRerun(true);
            }
            String [] loaderArgs = keeperArgs.toArray(new String[0]);
            if (!loader.runDiagnostics(loaderArgs, System.err)) {
                System.err.println("Diagnostics failed. Aborting execution.");
                System.exit(1);
            }
            loader.run(loaderArgs);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Invalid arguments: " + e.getMessage());
            new HelpFormatter().printHelp("PipelineLoader", options);
            System.exit(1);
        } catch (StageFailedException e) {
            System.err.println("Stage " + e.getStage().getName() + " failed with exit code " + e.getExitCode());
            System.exit(1);
        }
    }
}
