package org.wikibrain.cookbook.core;

import org.apache.commons.collections.IteratorUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Shilad Sen
 * Times the speed of the sparse matrix.
 */
public class LocalLinkTimer {
    private static final AtomicInteger numRows = new AtomicInteger();
    private static final AtomicLong numCells = new AtomicLong();
    private static final List<LocalId> localIds = new ArrayList<LocalId>();
    private static LocalLinkDao dao;
    private static Random random = new Random();

    public static void main(String args[]) throws ConfigurationException, DaoException, InterruptedException {
        Env env = EnvBuilder.envFromArgs(args);
        dao = env.getConfigurator().get(LocalLinkDao.class);
        LocalPageDao lpDao = env.getConfigurator().get(LocalPageDao.class);
        for (LocalPage p : (Iterable<LocalPage>)lpDao.get(new DaoFilter().setRedirect(false).setDisambig(false))) {
            localIds.add(p.toLocalId());
            if (localIds.size() % 100000 == 0) {
                System.err.println("added page " + localIds.size());
            }
        }
        List<Worker> workers = new ArrayList<Worker>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < WpThreadUtils.getMaxThreads(); i++) {
            Worker worker = new Worker();
            worker.start();
            workers.add(worker);
        }
        for (Worker worker : workers) {
            System.out.println("joining " + worker);
            worker.join();
        }
        System.out.println("elapsed is " + (System.currentTimeMillis() - start) + " rows is " + numRows.get() + " cells is " + numCells.get());
    }

    public static class Worker extends Thread {
        @Override
        public void run() {
            while (true) {
                LocalId id = localIds.get(random.nextInt(localIds.size()));
                try {
                    Iterable<LocalLink> cursor = dao.getLinks(id.getLanguage(), id.getId(), random.nextBoolean());
                    if (numRows.incrementAndGet() > 10000000) {
                        return;
                    }
                    numCells.addAndGet(IteratorUtils.toList(cursor.iterator()).size());
                } catch (DaoException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
