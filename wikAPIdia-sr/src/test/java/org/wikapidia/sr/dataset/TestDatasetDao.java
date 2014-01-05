package org.wikapidia.sr.dataset;

import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.sr.utils.KnownSim;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class TestDatasetDao {

    @Test
    public void testInfos() throws DaoException {
        Collection<DatasetDao.Info> infos = DatasetDao.readInfos();
        assertEquals(15, infos.size());
    }

    @Test
    public void testDaoRead() throws DaoException {
        DatasetDao dao = new DatasetDao();
        Dataset ds = dao.get("wordsim353.txt");
        assertEquals(351, ds.getData().size());
        assertEquals("en", ds.getLanguage().getLangCode());
        double sim = Double.NaN;
        for (KnownSim ks : ds.getData()) {
            if (ks.phrase1.equals("marriage") && ks.phrase2.equals("morality")) {
                sim = ks.similarity;
            }
        }
        assertTrue(!Double.isNaN(sim));
        assertEquals(sim, 0.354145342886, 0.000001);
    }
}
