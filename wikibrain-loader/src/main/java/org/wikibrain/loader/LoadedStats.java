package org.wikibrain.loader;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.model.MetaInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LoadedStats {
    private static final Logger LOG = LoggerFactory.getLogger(LoadedStats.class);
    private final MetaInfoDao dao;
    private final Env env;
    private static final int FIELD_WIDTH = 24;
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (MetaInfo mi : metaInfos) {
            printRow(
                    "" + component,
                    "" + mi.getLanguage(),
                    "" + mi.getNumRecords(),
                    "" + mi.getNumErrors(),
                    "" + df.format(mi.getLastUpdated())
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

    public static void main(String args[]) throws ConfigurationException, DaoException, InterruptedException {
        Env env = EnvBuilder.envFromArgs(args);
        if (env == null) {
            return;
        }
        LoadedStats ls = new LoadedStats(env);
        Thread.currentThread().sleep(1000);
        ls.print();
    }
}
