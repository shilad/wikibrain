package org.wikibrain.sr.evaluation;

import org.apache.commons.io.IOUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Shilad Sen
 */
public abstract class BaseEvaluationLog<T extends BaseEvaluationLog> implements Closeable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected final List<File> children = new ArrayList<File>();
    protected final Map<String, String> config;
    protected final BufferedWriter log;
    protected File logPath;

    protected int sucessful;
    protected int missing;
    protected int failed;
    protected Date startDate;


    public BaseEvaluationLog() throws IOException {
        this(new HashMap<String, String>(), null, new Date());
    }

    public BaseEvaluationLog(File logPath) throws IOException {
        this(new HashMap<String, String>(), logPath, new Date());
    }

    public BaseEvaluationLog(Map<String, String> config, File logPath) throws IOException {
        this(config, logPath, new Date());
    }

    public BaseEvaluationLog(Map<String, String> config, File logPath, Date date) throws IOException {
        this.config = config;
        this.logPath = logPath;
        this.startDate = date;
        if (logPath == null) {
            log = null;
        } else {
            log = WpIOUtils.openWriter(logPath);
            write("start\t" + formatDate(new Date()) + "\n");
            for (String key : config.keySet()) {
                write("config\t" + key + "\t" + config.get(key) + "\n");
            }
        }
    }

    protected static String formatDate(Date d) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(d);
        }
    }

    protected static Date parseDate(String s) throws ParseException {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.parse(s);
        }
    }

    public void setConfig(String field, String value) {
        this.config.put(field, value);
    }

    public int getMissing() {
        return missing;
    }

    public int getFailed() {
        return failed;
    }

    public int getSuccessful() {
        return sucessful;
    }

    public int getTotal() {
        return missing + failed + sucessful;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    /**
     * Writes a line, adds a newline, and flushes the log
     * @param line
     */
    protected synchronized void write(String line) throws IOException {
        if (log != null) {
            log.write(line + "\n");
            log.flush();
        }
    }


    /**
     * Return a textual summary of the evaluation as a map.
     * The summary includes: the config, date, total, failed, missing, successful, spearman, and pearson
     * The map is actually a LinkedHashMap, so if the config is ordered, it is preserved.
     * @return
     */
    public Map<String, String> getSummaryAsMap() {
        Map<String, String> summary = new LinkedHashMap<String, String>();
        summary.putAll(config);
        summary.put("date", startDate.toString());
        summary.put("total", Integer.toString(getTotal()));
        summary.put("failed", Integer.toString(failed));
        summary.put("missing", Integer.toString(missing));
        summary.put("successful", Integer.toString(sucessful));
        return summary;
    }

    /**
     * Merges the accumulated values in eval into
     * @param eval
     */
    public synchronized void merge(T eval) throws IOException {
        if (log != null && eval.logPath != null) {
            write("merge\t" + eval.logPath.getAbsolutePath());
        }
        if (eval.startDate.compareTo(startDate) > 0) {
            this.startDate = eval.startDate;
        }
        for (String key : (Set<String>)eval.config.keySet()) {
            if (!config.containsKey(key)) {
                config.put(key, (String)eval.config.get(key));
            }
        }
        missing += eval.missing;
        failed += eval.failed;
        sucessful += eval.sucessful;
        if (eval.logPath != null) {
            children.add(eval.logPath);
        }
        children.addAll(eval.children);
    }

    /**
     * Writes a summary of the results to the file.
     * @throws java.io.IOException
     */
    public void summarize(File path) throws IOException {
        BufferedWriter writer = WpIOUtils.openWriter(path);
        try {
            summarize(writer);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Writes a summary of the results to stdout.
     * @throws java.io.IOException
     */
    public void summarize() throws IOException {
        summarize(System.out);
    }

    /**
     * Writes a summary of the results to a printstream (probably System.out or System.in).
     * @throws java.io.IOException
     */
    public void summarize(PrintStream printStream) throws IOException {
        summarize(new BufferedWriter(new OutputStreamWriter(printStream)));
    }

    /**
     * Writes a summary of the results to the writer.               ;
     * @param writer
     * @throws java.io.IOException
     */
    public void summarize(BufferedWriter writer) throws IOException {
        for (Map.Entry<String, String> entry : getSummaryAsMap().entrySet()) {
            writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
        }
        writer.flush();
    }

    public List<File> getChildFiles() {
        return children;
    }

    public abstract List<T> getChildEvaluations() throws IOException, ParseException;

    @Override
    public void close() throws IOException {
        if (log != null) log.close();
    }

    public File getLogPath() {
        return logPath;
    }
}
