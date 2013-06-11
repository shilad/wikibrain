package org.wikapidia.core.dao.sql;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 */
public class UniversalPageSqlDao<T extends UniversalPage> implements UniversalPageDao<T> {

    @Override
    public void beginLoad() throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void save(T item) throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void endLoad() throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public T getById(int univId) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<Integer, T> getByIds(Collection<Integer> univIds) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
