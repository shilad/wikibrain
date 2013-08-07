package org.wikapidia.sr.utils;

import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: research
 * Date: 7/17/13
 * Time: 1:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestKnownSim {

    @Ignore
    @Test
    public void test() throws IOException, DaoException {

        String path = "/Users/research/IdeaProjects/wikapidia/dat/gold/cleaned/MC.txt";
        DatasetDao datasetDao = new DatasetDao();

        Dataset dataset = datasetDao.read(Language.getByLangCode("en"), path);

        for (KnownSim ks:dataset.getData()) {
            System.out.println(ks.toString());
        }


    }


}