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
    private static final Options options = new Options();
    static {
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

        EnvBuilder.addStandardOptions(options);
    }

    private PipelineLoader loader;
    private Env env;
    private CommandLine cmd;
    private String[] loaderArgs;

    public Loader(String args[]) throws ConfigurationException, DaoException, InterruptedException, ClassNotFoundException, ParseException {
        CommandLineParser parser = new PosixParser();
        cmd = parser.parse(options, args);

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


        env = new EnvBuilder(cmd).build();
        List<StageArgs> stageArgs = null;
        if (cmd.hasOption("s")) {
            stageArgs = new ArrayList<StageArgs>();
            for (String s : cmd.getOptionValues("s")) {
                stageArgs.add(new StageArgs(s));
            }
        }
        this.loader = new PipelineLoader(env, stageArgs);


        if (cmd.hasOption("d")) {
            loader.setForceRerun(true);
        }
        loaderArgs = keeperArgs.toArray(new String[0]);
    }

    public synchronized  void run() throws InterruptedException, IOException, StageFailedException {
        // Close and pause
        if (env != null) {
            env.close();
            Thread.sleep(1000);
            env = null;
        }

        loader.run(loaderArgs);
    }

    public synchronized boolean runDiagnostics() throws IOException, InterruptedException {
        return loader.runDiagnostics(env, loaderArgs, System.err);
    }

    public static void usage(String message, Exception e) {
        System.err.println(message + e.getMessage());
        new HelpFormatter().printHelp("Loader", options);
    }

    public PipelineLoader getLoader() {
        return loader;
    }

    public static void main(String args[]) throws ConfigurationException, ClassNotFoundException, IOException, InterruptedException, SQLException, DaoException {
        try {
            Loader loader = new Loader(args);
            if (!loader.runDiagnostics()) {
                System.err.println("Diagnostics failed. Aborting execution.");
                System.exit(1);
            }
            System.err.println("Beginning import process in 20 seconds...");
            Thread.sleep(5000);
            System.err.println("Beginning import process in 15 seconds...");
            Thread.sleep(5000);
            System.err.println("Beginning import process in 10 seconds...");
            Thread.sleep(5000);
            System.err.println("Beginning import process in 5 seconds...");
            Thread.sleep(5000);
            loader.run();
        } catch (ParseException e) {
            usage("Invalid arguments: ", e);
        } catch (IllegalArgumentException e) {
            usage("Invalid arguments: ", e);
        } catch (StageFailedException e) {
            System.err.println("Stage " + e.getStage().getName() + " failed with exit code " + e.getExitCode());
            System.exit(1);
        }
    }
}
