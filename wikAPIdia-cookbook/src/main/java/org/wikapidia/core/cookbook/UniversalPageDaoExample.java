package org.wikapidia.core.cookbook;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.dao.sql.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 */
public class UniversalPageDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Configurator configurator = new Configurator(new Configuration());
        UniversalPageDao pdao = configurator.get(UniversalPageDao.class);
        UniversalLinkDao ldao = configurator.get(UniversalLinkDao.class);

        int i = 0;
        for (UniversalPage page : (Iterable<UniversalPage>)pdao.get(new DaoFilter())) {
            System.out.println(page.getUnivId());
            i++;
        }
        System.out.println("Page count: " + i);
        i = 0;
        for (UniversalLink link : ldao.get(new DaoFilter().setSourceIds(0))) {
            System.out.println(link.getSourceUnivId() + " : " + link.getDestUnivId());
            i++;
        }
        System.out.println("Link count: " + i);
    }
}
