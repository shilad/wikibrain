package org.wikibrain.spatial.maxima;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by vonea001 on 7/14/14.
 */
public class ResultAnalyzer {

    private static Map<SpatialConceptPair,Set<ResponseLogLine>> pairsToResponses = new HashMap<SpatialConceptPair, Set<ResponseLogLine>>();
    private static Map<SpatialConceptPair,SpatialConceptPair> updater = new HashMap<SpatialConceptPair, SpatialConceptPair>();
    private static SpatialConcept.Scale[] converter = {null, null, SpatialConcept.Scale.LANDMARK, null,SpatialConcept.Scale.COUNTRY,
            SpatialConcept.Scale.STATE, SpatialConcept.Scale.CITY, SpatialConcept.Scale.NATURAL};

    public static void main (String args[]) throws IOException, ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Scanner scan = new Scanner(new File("questions.enhanced.tsv"));
        scan.nextLine();
        String header = "Label 1\tID 1\tLabel 2\tID 2\tDistance (km)\tDistance (graph)\tRelatedness\tExpected FF\tExpected FU\tExpected UU\tActual FF\tActual FU\tActual UU\tActual All\tAvg. FF\tAvg. FU\tAvg. UU\tAvg. All\tSR_Phrase\tSR_Page";
        Map<String,Set<ResponseLogLine>> byAuthor = new HashMap<String,Set<ResponseLogLine>> ();
        while(scan.hasNextLine()){
            String s = scan.nextLine();
            ResponseLogLine q = new ResponseLogLine(s);
            Set set = byAuthor.get(q.amazonId);
            if (set == null){
                set = new HashSet<ResponseLogLine>();
                set.add(q);
                byAuthor.put(q.amazonId,set);
            }else{
                set.add(q);
            }
        }

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter("results.tsv"));
        }catch(IOException e){
            e.printStackTrace();
        }
        pw.println(header);

        MonolingualSRMetric metric = env.getConfigurator().get(MonolingualSRMetric.class, "ensemble", "language", "en");
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);

        final int FF=0,FU=1,UU=2;
        for (SpatialConceptPair scp : pairsToResponses.keySet()) {
            System.out.println("doing " + scp.getFirstConcept().getTitle() + " and " + scp.getSecondConcept().getTitle());


            Set<ResponseLogLine> set = pairsToResponses.get(scp);
            int[] numbers = new int[3];
            double[] averages = new double[3];
            for (ResponseLogLine rll: set){
                if (rll.relatedness!=null && rll.relatedness>=0) {
                    if (rll.familiarity1 == null || rll.familiarity2 == null) {
                        continue;
                    }
                    if (rll.familiarity1 < 1 || rll.familiarity2 < 1) {
                        continue;
                    }
                    int index;
                    if (rll.familiarity1 > 3 && rll.familiarity2 > 3) {
                        index = FF;
                    } else if (rll.familiarity1 > 3 || rll.familiarity2 > 3) {
                        index = FU;
                    } else {
                        index = UU;
                    }
                    averages[index] = (numbers[index] * averages[index] + rll.relatedness) / (numbers[index] + 1.0);
                    numbers[index]++;
                }
            }
            if (numbers[0] + numbers[1] + numbers[2] >= 1) {
                List newCols = new ArrayList();
                int total = 0;
                for (int i = 0; i < numbers.length; i++) {
                    newCols.add(numbers[i]);
                    total += numbers[i];
                }
                newCols.add(total);
                double sum = 0;
                for (int i = 0; i < averages.length; i++) {
                    newCols.add(averages[i]);
                    sum += averages[i] * numbers[i];
                }
                newCols.add(sum / total);
                SRResult sr1 = metric.similarity(scp.getFirstConcept().getTitle(), scp.getSecondConcept().getTitle(), false);
                if (sr1 != null && sr1.isValid()) {
                    newCols.add(sr1.getScore());
                } else {
                    newCols.add(-1.0);
                }
                int page1 = pageDao.getIdByTitle(scp.getFirstConcept().getTitle(), Language.EN, NameSpace.ARTICLE);
                int page2 = pageDao.getIdByTitle(scp.getSecondConcept().getTitle(), Language.EN, NameSpace.ARTICLE);
                if (page1 >= 0 && page2 >= 0) {
                    SRResult sr2 = metric.similarity(page1, page2, false);
                    if (sr2 != null && sr2.isValid()) {
                        newCols.add(sr2.getScore());
                    } else {
                        newCols.add(-1.0);
                    }
                } else {
                    newCols.add(-1.0);
                }
                pw.println(scp.toString() + "\t" + StringUtils.join(newCols, '\t'));

            }
        }

        pw.close();
    }

    private static class ResponseLogLine {
        Integer grailsId;
        String amazonId;
        Integer page;
        Integer questionNumber;
        Float relatedness;
        Integer familiarity1, familiarity2, valence1, valence2;
        Integer pop1,	pop2;
        Boolean known1, known2;
        SpatialConceptPair pair;
        SpatialConcept first, second;

        public ResponseLogLine(String line){
            String[] array = line.split("\t");
            grailsId = parseInt(array[0]);
            amazonId = array[1];
            page = parseInt(array[2]);
            questionNumber = parseInt(array[3]);
            String location1Name = array[4];
            String location2Name = array[5];
            relatedness = parseFloat(array[6]);
            familiarity1 = parseInt(array[7]);
            familiarity2 = parseInt(array[8]);
            valence1 = parseInt(array[9]);
            valence2 = parseInt(array[10]);
            float km = parseFloat(array[11]);
            float graph = parseFloat(array[12]);
            float sr = parseFloat(array[13]);
            Integer wpId1 = parseInt(array[14]);
            Integer wpId2 = parseInt(array[15]);
            Integer scale1 = parseInt(array[16]);
            Integer scale2 = parseInt(array[17]);
            pop1 = parseInt(array[18]);
            pop2 = parseInt(array[19]);
            known1 = Boolean.parseBoolean(array[20]);
            known2 = Boolean.parseBoolean(array[21]);

            first = new SpatialConcept(wpId1,location1Name);
            second = new SpatialConcept(wpId2,location2Name);
            pair = new SpatialConceptPair(first,second);
            SpatialConceptPair official = updater.get(pair);
            Set<ResponseLogLine> set = pairsToResponses.get(pair);
            if (official == null){
                official = pair;
                official.setGraphDistance(graph);
                official.setKmDistance(km);
                official.setRelatedness(sr);
                official.getFirstConcept().setScale(converter[scale1+1]);
                official.getSecondConcept().setScale(converter[scale2+1]);
                set = new HashSet<ResponseLogLine>();
            }
            if (known1 && known2){
                official.increaseKkNumbOfTimesAsked(1);
            }else if (known1 || known2){
                official.increaseKuNumbOfTimesAsked(1);
            }else{
                official.increaseUuNumbOfTimesAsked(1);
            }
            pair = official;
            updater.put(official, official);
            set.add(this);
            pairsToResponses.put(official, set);
        }
    }

    private static Integer parseInt(String str){
        try{
            return Integer.parseInt(str);
        }catch(NumberFormatException e){
            return null;
        }
    }

    private static Float parseFloat(String str){
        try{
            return Float.parseFloat(str);
        }catch(NumberFormatException e){
            return null;
        }
    }
}
