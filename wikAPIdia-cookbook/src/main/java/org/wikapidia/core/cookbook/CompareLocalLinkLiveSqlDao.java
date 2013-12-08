package org.wikapidia.core.cookbook;

import au.com.bytecode.opencsv.CSVWriter;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.NameSpace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * An Example shows the difference of results between LocalLinkLiveDao & LocalLinkSqlDao
 * @author Toby "Jiajun" Li
 */

public class CompareLocalLinkLiveSqlDao {

    static long liveInCounter = 0, liveOutCounter = 0, sqlInCounter = 0, sqlOutCounter = 0, commonInCounter = 0, commonOutCounter = 0;
    static Set<Integer> inLive = new HashSet();
    static Set<Integer> outLive = new HashSet();
    static Set<Integer> inCommon = new HashSet();
    static Set<Integer> outCommon = new HashSet();



    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalLinkDao ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        LocalPageDao pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        Language lang = Language.getByLangCode("simple");
        int pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));

        File f=new File("../wikAPIdia-cookbook/linkstat.csv");
        String[] entries = new String[3];
        CSVWriter csvWriter = new CSVWriter(new FileWriter(f), ',');


        Iterable<LocalLink> inlinks = ldao.getLinks(lang, pageId, false);

        for (LocalLink inlink : inlinks) {
            liveInCounter++;
            inLive.add(inlink.getSourceId());
            //System.out.println(inlink);
        }

        Iterable<LocalLink> outlinks = ldao.getLinks(lang, pageId, true);

        for (LocalLink outlink : outlinks) {
            liveOutCounter++;
            outLive.add(outlink.getDestId());
            //System.out.println(outlink);
        }

        ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "sql");
        pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");
        pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));
        ;

        inlinks = ldao.getLinks(lang, pageId, false);

        for (LocalLink inlink : inlinks) {
            sqlInCounter++;
            if(inLive.contains(inlink.getSourceId())){
                inCommon.add(inlink.getSourceId());
                commonInCounter++;
            }
            else{
                entries[0] = "LiveDao Failed to get inbound link ";
                entries[1] = inlink.toString();
                csvWriter.writeNext(entries);
            }
            //System.out.println(inlink);
        }

        outlinks = ldao.getLinks(lang, pageId, true);

        for (LocalLink outlink : outlinks) {
            sqlOutCounter++;
            if(outLive.contains(outlink.getDestId())){
                outCommon.add(outlink.getDestId());
                commonOutCounter++;
            }
            else{
                entries[0] = "LiveDao Failed to get outbound link ";
                entries[1] = outlink.toString();
                csvWriter.writeNext(entries);
            }
            //System.out.println(outlink);
        }
        ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        inlinks = ldao.getLinks(lang, pageId, false);

        for (LocalLink inlink : inlinks) {
            if(!inCommon.contains(inlink)){
                entries[0] = "SQLDao Failed to get inbound link ";
                entries[1] = inlink.toString();
                csvWriter.writeNext(entries);
            }
        }

        outlinks = ldao.getLinks(lang, pageId, true);

        for (LocalLink outlink : outlinks) {
            if(!outCommon.contains(outlink)){
                entries[0] = "SQLDao Failed to get outbound link ";
                entries[1] = outlink.toString();
                csvWriter.writeNext(entries);
            }
        }




        System.out.printf("\nNumber of inlinks in LiveDao: %d\nNumber of inlinks in SQLDao: %d\nNumber of inlinks in common: %d\n\nNumber of outlinks in LiveDao: %d\n" +
                "Number of outlinks in SQLDao: %d\n" +
                "Number of outlinks in common: %d\n", liveInCounter, sqlInCounter, commonInCounter, liveOutCounter, sqlOutCounter, commonOutCounter);
        System.out.println("Detailed error information is printed to linkstat.csv at wikAPIdia-cookbook directory");
        entries = String.format("Number of inlinks in LiveDao: %d#Number of inlinks in SQLDao: %d#Number of inlinks in common: %d", liveInCounter, sqlInCounter, commonInCounter).split("#");
        csvWriter.writeNext(entries);
        entries = String.format("Number of outlinks in LiveDao: %d#Number of outlinks in SQLDao: %d#Number of outlinks in common: %d", liveOutCounter, sqlOutCounter, commonOutCounter).split("#");
        csvWriter.writeNext(entries);
        csvWriter.close();
    }

}