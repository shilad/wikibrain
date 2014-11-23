package org.wikibrain.cookbook.sr;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;

import java.util.*;

/**
 * Created by toby on 10/1/14.
 */
public class GroupSimilarity {
    public static void main(String[] args) throws Exception{
        // Initialize the WikiBrain environment and get the local page dao
        Env env = new EnvBuilder().envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Language lang = Language.getByLangCode("en");

        // Retrieve the "ensemble" sr metric for simple english
        SRMetric sr = conf.get(
                SRMetric.class, "ensemble",
                "language", lang.getLangCode());

        //Similarity between strings
        final Map<Integer, Double> neighborhoodSRMap = new HashMap<Integer, Double>();
        List<Integer> mnNeighborhoods = Arrays.asList(235683, 7048930, 544485, 4665512, 8094477, 7082376, 11228229, 6503675, 1220709, 12924642, 2152716, 7050415, 7012740, 3129388, 12921800, 7067320, 1141979);
        Integer target = 131123;
        for (Integer neighborhood : mnNeighborhoods ) {
            SRResult srResult = sr.similarity(neighborhood, target, true);
            neighborhoodSRMap.put(neighborhood, srResult.getScore());
            //System.out.println("Similarity between " + lpDao.getById(lang, neighborhood).getTitle().getCanonicalTitle() + " and " + lpDao.getById(lang, target).getTitle().getCanonicalTitle() + " is " + srResult.getScore() );
            }
        Collections.sort(mnNeighborhoods, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if(neighborhoodSRMap.get(o1) < neighborhoodSRMap.get(o2))
                    return 1;
                else
                    return -1;
            }
        });
        for(Integer neighborhood : mnNeighborhoods){
            System.out.println("Similarity between " + lpDao.getById(lang, neighborhood).getTitle().getCanonicalTitle() + " and " + lpDao.getById(lang, target).getTitle().getCanonicalTitle() + " is " + neighborhoodSRMap.get(neighborhood) );
        }
        }


    }






