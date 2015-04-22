package org.wikibrain.sr.phrasesim;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class PhraseSimEvaluator {
    private boolean debug = true;
    private static List<List<String>> TEST_BUNDLES = Arrays.asList(
            makeSet("jazz music blues"),
            makeSet("music math statistics"),
            makeSet("music brain"),
            makeSet("brain mind"),
            makeSet("brain statistics algorithm")
    );
    private int k = 10;

    private final Env env;

    public PhraseSimEvaluator(Env env) {
        this.env = env;
    }

    public void evaluate(final List<List<String>> bundles) throws ConfigurationException, IOException {
        String lc = env.getDefaultLanguage().getLangCode();
        File dir = FileUtils.getFile(env.getBaseDir(), "dat/sr/known-phrase/en");
        FileUtils.deleteQuietly(dir);

        final KnownPhraseSim sim = (KnownPhraseSim) env.getConfigurator().get(SRMetric.class, "known-phrase", "language", lc);
        if (!sim.getDataDir().equals(dir)) {
            throw new IllegalStateException("expected dir " + dir + ", found " + sim.getDataDir());
        }
        final Map<String, Integer> ids = new ConcurrentHashMap<String, Integer>();
        ParallelForEach.loop(bundles, new Procedure<List<String>>() {
            @Override
            public void call(List<String> bundle) throws Exception {
                for (String phrase : bundle) {
                    String s = sim.normalize(phrase);
                    if (!ids.containsKey(s)) {
                        ids.put(s, ids.size());
                    }
                    int id = ids.get(s);
                    sim.addPhrase(phrase, id);
                }
            }
        });

        sim.flushCosimilarity();
        sim.trainNormalizer();

        int numSamples = 0;
        int numSampleHits = 0;
        int numRecommended = 0;
        int numRecommendedHits = 0;
        int possible = 0;
        int numErrors = 0;

        long before = System.currentTimeMillis();
        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            // Select a random bundle
            List<String> bundle = bundles.get(rand.nextInt(bundles.size()));
            if (bundle.isEmpty()) {
                continue;
            }
            numSamples++;
            TIntSet bundleIds = new TIntHashSet();
            for (String p : bundle) {
                bundleIds.add(ids.get(sim.normalize(p)));
            }
            String target = bundle.iterator().next();
            int targetId = ids.get(sim.normalize(target));
            int j = 0;
            boolean hasHit = false;
            StringBuffer line = new StringBuffer(target).append(": ");
            SRResultList neighbors = sim.mostSimilar(target, k + 1);
            if (neighbors == null) {
                numErrors++;
                continue;
            }
            for (SRResult r : neighbors) {
                if (r.getId() != targetId) {
                    if (this.debug) line.append(
                            String.format("%s %.3f, ",
                                    sim.getPhrase(r.getId()), r.getScore()));
                    if (bundleIds.contains(r.getId())) {
                        hasHit = true;
                        numRecommendedHits++;
                    }
                    numRecommended++;
                    if (++j >= k) {
                        break;
                    }
                }
            }
            if (this.debug) System.out.println(line);
            possible += bundleIds.size();
            if (bundleIds.contains(targetId)) {
                possible--;
            }
            if (hasHit) {
                numSampleHits++;
            }
        }
        long after = System.currentTimeMillis();

        System.out.println("for " + bundles.size() + ", top " + k);
        System.out.println("Total samples: " + numSamples);
        System.out.println("Total errors: " + numErrors);
        System.out.println("Total seconds: " + ((after - before) / 1000.0));
        System.out.println("Total samples with hits: " + numSampleHits);
        System.out.println("Total related items: " + numRecommended);
        System.out.println("Total related items with hits: " + numRecommendedHits);
        System.out.println("Precision: " + (1.0 * numRecommendedHits / numRecommended));
        System.out.println("Recall: " + (1.0 * numRecommendedHits / possible));
    }

    public void setTopK(int k) {
        this.k = k;
    }

    static private List<String> makeSet(String line) {
        return new ArrayList<String>(Arrays.asList(line.split(" ")));
    }

    public static List<List<String>> readBundles(File f) throws IOException {
        List<List<String>> bundles = new ArrayList<List<String>>();
        for (String line : FileUtils.readLines(f)) {
            List<String> bundle = new ArrayList<String>();
            for (String token : line.split("\t")) {
                bundle.add(token.trim());
            }
            if (bundle.size() >= 2) {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    public static void main(String args[]) throws ConfigurationException, IOException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("bundles")
                        .withDescription("bundle file with tab separated phrases")
                        .hasArg()
                        .create("b"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("topk")
                        .withDescription("number neighbors per phrase")
                        .hasArg()
                        .create("k"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("PhraseSimEvaluator", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();

        PhraseSimEvaluator eval = new PhraseSimEvaluator(env);
        List<List<String>> bundles;

        if (cmd.hasOption("b")) {
            bundles = readBundles(new File(cmd.getOptionValue("b")));
        } else {
            bundles = TEST_BUNDLES;
        }

        if (cmd.hasOption("k")) {
            eval.setTopK(Integer.parseInt(cmd.getOptionValue("k")));
        }
        eval.evaluate(bundles);
    }
}
