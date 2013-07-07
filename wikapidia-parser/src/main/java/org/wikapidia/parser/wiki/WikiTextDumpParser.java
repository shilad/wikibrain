package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class WikiTextDumpParser {
    public static final Logger LOG = Logger.getLogger(WikiTextDumpParser.class.getName());

    // maximum number of raw pages in the parsing buffer
    public static final int MAX_QUEUE = 1000;

    private final LanguageInfo language;
    private final RawPageDao rawPageDao;
    private final LanguageSet allowedLanguages;
    private int maxThreads = Runtime.getRuntime().availableProcessors();
    private final BlockingQueue<RawPage> queue = new ArrayBlockingQueue<RawPage>(MAX_QUEUE);
    private final List<Thread> workers = new ArrayList<Thread>();
    private AtomicBoolean finished = new AtomicBoolean(false);

    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language) {
        this(rawPageDao, language, null);
    }

    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language, LanguageSet allowedIllLangs) {
        this.language = language;
        this.allowedLanguages = allowedIllLangs;
        this.rawPageDao = rawPageDao;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Parses the input file completely. First splits the file into individual PageXmls via
     * DumpPageXmlParser, then parses each page via WikiTextParser
     *
     * @param visitor extracts data from side effects
     */
    public void parse(ParserVisitor visitor) throws DaoException {
        parse(Arrays.asList(visitor));
    }

    public synchronized void parse(List<ParserVisitor> visitors) throws DaoException {
        DaoFilter daoFilter = new DaoFilter().setLanguages(language.getLanguage());
        try {
            createWorkers(visitors);
            for(RawPage page : rawPageDao.get(daoFilter)) {
                try {
                    queue.put(page);
                } catch (InterruptedException e) {
                    LOG.log(Level.WARNING, "master parser interrupted, breaking:", e);
                    break;
                }
            }
        } finally {
            cleanupWorkers();
        }
    }

    private void createWorkers(List<ParserVisitor> visitors) {
        workers.clear();
        finished.set(false);
        for (int i = 0; i < maxThreads; i++) {
            Thread t = new Thread(new Worker(finished, visitors));
            t.start();
            workers.add(t);
        }
    }

    private void cleanupWorkers() {
        finished.set(false);
        for (Thread w : workers) {
            try {
                w.join(1000);
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "ignoring interrupted exception on thread join", e);
            }
        }
        for (Thread w : workers) {
            w.interrupt();
        }
    }

    private class Worker implements Runnable {
        private final AtomicBoolean finished;
        private final WikiTextParser parser;

        public Worker(AtomicBoolean finished, List<ParserVisitor> visitors) {
            this.parser = new WikiTextParser(language, allowedLanguages, visitors);
            this.finished = finished;
        }
        @Override
        public void run() {
            RawPage rp = null;
            while (!finished.get()) {
                try {
                    rp = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (rp != null) {
                        parser.parse(rp);
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.WARNING, "WikiTextDumpParser.Worker received interrupt.");
                    return;
                } catch (Exception e) {
                    String title = "unknown";
                    if (rp != null) title = rp.getTitle().toString();
                    LOG.log(Level.WARNING, "exception while parsing " + title, e);
                }
            }
        }
    }
}
