import com.jolbox.bonecp.BoneCPDataSource;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.dao.sql.RedirectSqlDao;
import org.wikapidia.core.lang.Language;

import javax.sql.DataSource;
import java.io.File;

/**
 */
public class LoaderTest {

    @Test
    public void test() throws DaoException{
        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:db/h2");
        ds.setUsername("sa");
        ds.setPassword("");
        RedirectSqlDao redirectSqlDao = new RedirectSqlDao(ds);
        LocalPageSqlDao localPageSqlDao = new LocalPageSqlDao(ds, false);

        System.out.println("I got here.");
        Language language = Language.getByLangCode("la");
        TIntIntMap map = redirectSqlDao.getAllRedirectIdsToDestIds(language);
        for(int src : map.keys()){
            String srcTitle = localPageSqlDao.getById(language, src).getTitle().getCanonicalTitle();
            String destTitle = localPageSqlDao.getById(language, map.get(src)).getTitle().getCanonicalTitle();
            System.out.println(srcTitle + " --> " + destTitle);
        }

    }
}
