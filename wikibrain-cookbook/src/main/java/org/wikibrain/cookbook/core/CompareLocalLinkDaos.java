package org.wikibrain.cookbook.core;

import org.apache.commons.collections.CollectionUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.core.model.UniversalPage;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class CompareLocalLinkDaos {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalLinkDao sqlDao = configurator.get(LocalLinkDao.class, "sql");
        LocalLinkDao matrixDao = configurator.get(LocalLinkDao.class, "matrix");

        Map<LocalId, Set<LocalId>> outGraph = new HashMap<LocalId, Set<LocalId>>();
        Map<LocalId, Set<LocalId>> inGraph = new HashMap<LocalId, Set<LocalId>>();

        long start = System.currentTimeMillis();
        int i = 0;
        for (LocalLink ll : sqlDao.get(new DaoFilter())) {
            if (ll.getSourceId() < 0 || ll.getDestId() < 0) {
                continue;
            }
            LocalId src = new LocalId(ll.getLanguage(), ll.getSourceId());
            LocalId dest = new LocalId(ll.getLanguage(), ll.getDestId());
            if (!outGraph.containsKey(src)) {
                outGraph.put(src, new HashSet<LocalId>());
            }
            outGraph.get(src).add(dest);
            if (!inGraph.containsKey(dest)) {
                inGraph.put(dest, new HashSet<LocalId>());
            }
            inGraph.get(dest).add(src);
            i++;
        }
        double elapsed = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("counted " + i + " links in sql dao in " + elapsed + " secs");

        start = System.currentTimeMillis();
        int different = 0;
        int adds = 0;
        int dels = 0;
        for (LocalId src : outGraph.keySet()) {
            Set<LocalId> expected = outGraph.get(src);
            Set<LocalId> actual = new HashSet<LocalId>();
            for (LocalLink ll : matrixDao.getLinks(src.getLanguage(), src.getId(), true)) {
                if (ll.getSourceId() < 0 || ll.getDestId() < 0) {
                    continue;
                }
                actual.add(new LocalId(ll.getLanguage(), ll.getDestId()));
            }
            if (!actual.equals(expected)) {
                System.out.println("comparing " + actual + " and " + expected);
                different++;
            }
            adds += retainedSize(actual, expected);
            dels += retainedSize(expected, actual);
        }
        elapsed = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("verified " + i + " links in matrix dao in " + elapsed + " secs");
        System.out.println("different " + different);
        System.out.println("adds " + adds);
        System.out.println("dels " + dels);
    }

    private static int retainedSize(Set orig, Set toRemove) {
        Set copy = new HashSet(orig);
        copy.removeAll(toRemove);
        return copy.size();
    }
}
