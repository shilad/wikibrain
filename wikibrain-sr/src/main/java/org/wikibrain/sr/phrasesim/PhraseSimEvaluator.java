package org.wikibrain.sr.phrasesim;

import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.VectorBasedSRMetric;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class PhraseSimEvaluator {
    private static List<Set<String>> TEST_BUNDLES = Arrays.asList(
            makeSet("jazz music blues"),
            makeSet("music math statistics"),
            makeSet("music brain"),
            makeSet("brain mind"),
            makeSet("brain statistics algorithm")
    );

    private final Env env;

    public PhraseSimEvaluator(Env env) {
        this.env = env;
    }

    public void evaluate(List<Set<String>> bundles) throws ConfigurationException, IOException {
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "ESA", "language", "simple");
        StringNormalizer normalizer = env.getConfigurator().get(StringNormalizer.class, "simple");
        SimplePhraseCreator creator = new SimplePhraseCreator((VectorBasedSRMetric) metric);
        File dir = FileUtils.getFile(env.getBaseDir(), "dat/phrase-sim-test/");
        FileUtils.deleteQuietly(dir);
        dir.mkdirs();

        KnownPhraseSim sim = new KnownPhraseSim(creator, dir, normalizer);
        Map<String, Integer> ids = new HashMap<String, Integer>();
        for (Set<String> bundle : bundles) {
            for (String phrase : bundle) {
                String s = sim.normalize(phrase);
                if (!ids.containsKey(s)) {
                    ids.put(s, ids.size());
                }
                int id = ids.get(s);
                sim.addPhrase(phrase, id);
            }
        }
    }


    static private Set<String> makeSet(String line) {
        return new HashSet<String>(Arrays.asList(line.split(" ")));
    }

    public static void main(String args[]) throws ConfigurationException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        PhraseSimEvaluator eval = new PhraseSimEvaluator(env);
        eval.evaluate(TEST_BUNDLES);
    }
}
