package org.wikapidia.core.dao;

import org.junit.Test;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.sql.LocalCategoryMemberSqlDao;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 */
public class TestLocalCategoryMemberDao {

    @Test
    public void testMembership() throws ClassNotFoundException, IOException, DaoException, WikapidiaException {
        DataSource ds = TestDaoUtil.getDataSource();
        LanguageInfo lang = LanguageInfo.getByLangCode("en");

        LocalCategoryMemberSqlDao dao = new LocalCategoryMemberSqlDao(ds);
        LocalPageSqlDao pageDao = new LocalPageSqlDao(ds);

        dao.beginLoad();
        pageDao.beginLoad();
        List<LocalCategory> localCategories = new ArrayList<LocalCategory>();

        /*
        This giant for-loop establishes a set of categories and articles and their relationship.
        It generates 100 articles labeled 1 through 100 with IDs 101 through 200, and
        25 categories corresponding to the 25 prime numbers that are less than 100,
        stored as their value. An article is in a category if its value (1-100) is divisible by
        the corresponding value of the category.
         */
        for (int i=1; i<=100; i++) {
            if (isPrime(i)) {
                LocalCategory localCategory = new LocalCategory(
                        lang.getLanguage(),
                        i,
                        new Title("Category " + i, lang)
                );
                localCategories.add(localCategory);
                pageDao.save(localCategory);
            }
            LocalArticle localArticle = new LocalArticle(
                    lang.getLanguage(),
                    i+100,
                    new Title("Article " + i, lang)
            );
            for (LocalCategory localCategory : localCategories) {
                if (i%localCategory.getLocalId() == 0) {
                    dao.save(localCategory, localArticle);
                }
            }
            pageDao.save(localArticle);
        }
        dao.endLoad();
        pageDao.endLoad();

        Map<Integer, LocalArticle> map = dao.getCategoryMembers(lang.getLanguage(), 5);
        assertEquals(20, map.size());
        map = dao.getCategoryMembers(lang.getLanguage(), 7);
        assertEquals(100/7, map.size());

        Map<Integer, LocalCategory> map2 = dao.getCategories(lang.getLanguage(), 42+100);
        assertEquals(3, map2.size());
        map2 = dao.getCategories(lang.getLanguage(), 81+100);
        assertEquals(1, map2.size());
        assert (map2.containsKey(3));
    }

    private boolean isPrime(int n) {
        if (n<2) return false;
        for (int i=2; i<=n/2; i++) {
            if (n%i == 0) return false;
        }
        return true;
    }
}
