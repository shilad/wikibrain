package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.lucene.LuceneIndexer;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.wiki.WikiTextParser;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;
import org.wikapidia.utils.WpThreadUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This loader indexes raw pages into the lucene index.
 * It should not be called sooner than the WikiTextLoader,
 * but where after that I am not sure.
 *
 * @author Ari Weiland
 *
 */
public class LuceneLoader {
    private static final Logger LOG = Logger.getLogger(LuceneLoader.class.getName());

    // maximum number of raw pages in the parsing buffer
    public static final int MAX_QUEUE = 1000;

    private final RawPageDao rawPageDao;
    private final LuceneIndexer luceneIndexer;
    private final Collection<NameSpace> namespaces;


    private final BlockingQueue<RawPage> queue = new ArrayBlockingQueue<RawPage>(MAX_QUEUE);
    private final List<Thread> workers = new ArrayList<Thread>();
    private AtomicBoolean finished = new AtomicBoolean(false);

    public LuceneLoader(RawPageDao rawPageDao, LuceneIndexer luceneIndexer, Collection<NameSpace> namespaces) {
        this.rawPageDao = rawPageDao;
        this.luceneIndexer = luceneIndexer;
        this.namespaces = namespaces;
    }

    public void load(Language language) throws WikapidiaException {
        try {
            createWorkers();
            int i = 0;
            Iterable<RawPage> rawPages = rawPageDao.get(new DaoFilter()
                    .setLanguages(language)
                    .setNameSpaces(namespaces));
            for (RawPage rawPage : rawPages) {
                queue.put(rawPage);
                if (++i%1000 == 0) LOG.log(Level.INFO, "RawPages indexed: " + i);
            }
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        } catch (InterruptedException e) {
            throw new WikapidiaException(e);
        } finally {
            cleanupWorkers();
        }
    }

    public void endLoad() {
        luceneIndexer.close();
    }

    private void createWorkers() {
        workers.clear();
        finished.set(false);
        for (int i = 0; i < WpThreadUtils.getMaxThreads(); i++) {
            Thread t = new Thread(new Worker());
            t.start();
            workers.add(t);
        }
    }

    private void cleanupWorkers() {
        finished.set(true);
        for (Thread w : workers) {
            try {
                w.join(10000);
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "ignoring interrupted exception on thread join", e);
            }
        }
        for (Thread w : workers) {
            w.interrupt();
        }
    }

    private class Worker implements Runnable {
        public Worker() { }
        @Override
        public void run() {
            RawPage rp = null;
            while (!finished.get()) {
                try {
                    rp = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (rp != null) {
                        luceneIndexer.indexPage(rp);
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.WARNING, "LuceneLoader.Worker received interrupt.");
                    return;
                } catch (Exception e) {
                    String title = "unknown";
                    if (rp != null) title = rp.getTitle().toString();
                    LOG.log(Level.WARNING, "exception while parsing " + title, e);
                }
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, WikapidiaException, IOException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-indexes")
                        .withDescription("drop and recreate all indexes")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("namespaces")
                        .withDescription("the set of namespaces to index, separated by commas")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("indexes")
                        .withDescription("the types of indexes to store, separated by commas")
                        .create("i"));
        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("LuceneLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();

        LuceneOptions[] luceneOptions;
        if (cmd.hasOption("i")) {
            String[] optionType = cmd.getOptionValues("i");
            luceneOptions = new LuceneOptions[optionType.length];
            for (int i=0; i<optionType.length; i++) {
                luceneOptions[i] = conf.get(LuceneOptions.class, optionType[i]);
            }
        } else {
            luceneOptions = new LuceneOptions[] {
                    conf.get(LuceneOptions.class, "plaintext"),
                    conf.get(LuceneOptions.class, "esa")
            };
        }

        LanguageSet languages = env.getLanguages();
        Collection<NameSpace> namespaces = new ArrayList<NameSpace>();
        if (cmd.hasOption("p")) {
            String[] nsStrings = cmd.getOptionValues("p");
            for (String s : nsStrings) {
                namespaces.add(NameSpace.getNameSpaceByName(s));
            }
        } else {
            namespaces = luceneOptions[0].namespaces;
        }
        RawPageDao rawPageDao = conf.get(RawPageDao.class);

        LuceneIndexer luceneIndexer = new LuceneIndexer(languages, luceneOptions);
        final LuceneLoader loader = new LuceneLoader(rawPageDao, luceneIndexer, namespaces);

        LOG.log(Level.INFO, "Begin indexing");

        // TODO: parallelize by some more efficient method?
        ParallelForEach.loop(
                languages.getLanguages(),
                new Procedure<Language>() {
                    @Override
                    public void call(Language language) throws Exception {
                        loader.load(language);
                    }
                }
        );

        loader.endLoad();
        LOG.log(Level.INFO, "Done indexing");
    }
}
