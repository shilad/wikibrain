package org.wikapidia.dao.load;

import org.apache.commons.lang3.StringUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.model.MetaInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 */
public class LoadedStats {
    private static final Logger LOG = Logger.getLogger(LoadedStats.class.getName());
    private final MetaInfoDao dao;
    private final Env env;
    private static final int FIELD_WIDTH = 28;
    private static final int WIDTH = FIELD_WIDTH * 5 + 2 - 1;

    public LoadedStats(Env env) throws ConfigurationException {
        this.env = env;
        this.dao = env.getConfigurator().get(MetaInfoDao.class);
    }

    public void print() throws DaoException {
        System.err.flush();

        printHeader();

        Map<String, List<MetaInfo>> allInfo = dao.getAllInfo();
        List<String> componentNames = new ArrayList<String>(allInfo.keySet());
        Collections.sort(componentNames);

        for (String component : componentNames) {
            printComponent(component, allInfo.get(component));
        }

        printFooter();
    }

    private void printComponent(String component, List<MetaInfo> metaInfos) {
        for (MetaInfo mi : metaInfos) {
            printRow(
                    "" + component,
                    "" + mi.getLanguage(),
                    "" + mi.getNumRecords(),
                    "" + mi.getNumErrors(),
                    "" + mi.getLastUpdated()
                );
        }
    }


    private void printHeader() {
        System.out.println(StringUtils.repeat("*", WIDTH));
        System.out.println("*" + StringUtils.repeat(" ", WIDTH - 2) + "*");
        System.out.println("*" + StringUtils.center("LOADED WIKIBRAIN DATA:", WIDTH - 2) + "*");
        System.out.println("*" + StringUtils.repeat(" ", WIDTH - 2) + "*");
        System.out.println("*" + StringUtils.center(" Default language: " + env.getLanguages().getDefaultLanguage(), WIDTH - 2) + "*");
        System.out.println("*" + StringUtils.center(" Loaded languages: " + env.getLanguages(), WIDTH - 2) + "*");
        System.out.println("*" + StringUtils.repeat(" ", WIDTH - 2) + "*");
        System.out.println(StringUtils.repeat("-", WIDTH));
        printRow("component", "language", "count", "errors", "modified");
        System.out.println(StringUtils.repeat("-", WIDTH));
    }

    private void printFooter() {
        System.out.println(StringUtils.repeat("-", WIDTH));
    }

    private void printRow(String ... fields) {
        System.out.print("|");
        for (String field : fields) {
            System.out.print(StringUtils.leftPad(field, FIELD_WIDTH - 2) + " |");
        }
        System.out.println("");
    }

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        if (env == null) {
            return;
        }
        LoadedStats ls = new LoadedStats(env);
        ls.print();
    }
}
