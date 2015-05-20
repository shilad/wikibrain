package org.wikibrain.utils;

import org.apache.commons.io.FilenameUtils;
import org.clapper.util.classutil.*;
import org.wikibrain.conf.ProviderFilter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * @author Shilad Sen
 */
public class JvmUtils {
    /**
     * Wikibrain + the standard Geometry class
     */
    private static Pattern WIKIBRAIN_CLASS_PATTERN = Pattern.compile("org.wikibrain.*|com.vividsolutions.jts.geom.Geometry");

    /**
     * Ignores Jooq and anonymous classes.
     */
    private static Pattern WIKIBRAIN_CLASS_BLACKLIST = Pattern.compile("(.*jooq.*|\\$[0-9]+$)");

    private static final Logger LOG = LoggerFactory.getLogger(JvmUtils.class);

    public static final int MAX_FILE_SIZE = 8 * 1024 * 1024;   // 8MB


    /**
     * Returns a carefully constructed classpath matching the current process.
     * @return
     */
    public static String getClassPath() {
        String separator = System.getProperty("path.separator");
        String classPath = System.getProperty("java.class.path", ".");

        // Try to get the URL class loader to make dynamic class loading work for Grails, etc.
        ClassLoader loader = JvmUtils.class.getClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader)loader).getURLs()) {
                    if (isLocalFile(url)) {
                        try {
                            classPath += separator + new File(url.toURI());
                        } catch (URISyntaxException e) {
                            LOG.warn("Illegal url: " + url);
                        }
                    }
                }
            }
            loader = loader.getParent();
        }
        return classPath;
    }

    private static List<File> CLASS_PATH = null;
    public synchronized static List<File> getClassPathAsList() {
        if (CLASS_PATH != null) {
            return CLASS_PATH;
        }
        CLASS_PATH = new ArrayList<File>();
        String separator = System.getProperty("path.separator");
        for (String entry : getClassPath().split(separator)) {
            LOG.debug("considering classpath entry " + entry);
            File file = new File(entry);
            if (!file.exists()) {
                LOG.warn("skipping nonexistent classpath file " + file);
                continue;
            }
            CLASS_PATH.add(new File(FilenameUtils.normalize(file.getAbsolutePath())));
        }
        return CLASS_PATH;
    }

    /**
     * Launches a new java program that uses the running configuration settings.
     * @param klass
     * @param args
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static Process launch(Class klass, String args[]) throws IOException, InterruptedException {
        return launch(klass, args, System.out, System.err, null);
    }

    /**
     * Launches a new java program that uses the running configuration settings.
     * @param klass
     * @param args
     * @param out stdout stream for process
     * @param err stderr stream for process
     * @param heapSize Maximum heap memory (e.g. 4M, 4G, etc) or null to use existing setting.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static Process launch(Class klass, String args[], OutputStream out, OutputStream err, String heapSize) throws IOException, InterruptedException {
        JavaProcessBuilder builder = new JavaProcessBuilder();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();


        String heapArg = heapSize == null ? null : ("-Xmx" + heapSize);
        boolean foundHeapArg = false;
        for (String jvmArg : runtimeMxBean.getInputArguments()) {
            if (!jvmArg.startsWith("-")) {
                break;
            }
            if (heapArg != null && jvmArg.startsWith("-Xmx")) {
                foundHeapArg = true;
                jvmArg = heapArg;
            }
            if (!jvmArg.equals("-jar")) {
                builder.jvmArg(jvmArg);
            }
        }
        if (heapArg != null && !foundHeapArg) {
            builder.jvmArg(heapArg);
        }

        builder.classpath(getClassPath());
        for (String arg : args) {
            builder.arg(arg);
        }
        builder.mainClass(klass.getName());
        return builder.launch(out, err);
    }

    /**
     * Adapted from http://www.java2s.com/Code/Java/Network-Protocol/IsURLalocalfile.htm
     * @param url
     * @return
     */
    private static boolean isLocalFile(URL url) {
        return ((url.getProtocol().equals("file"))
        &&      (url.getHost() == null || url.getHost().equals("")));
    }

    /**
     * @return The maximum JVM heap size in Mbs, rounded down.
     */
    private static int maxMemoryInMbs() {
        return (int) (Runtime.getRuntime().maxMemory() / (1024*1024));
    }


    public static void setWikiBrainClassPattern(Pattern pattern, Pattern blacklist) {
        WIKIBRAIN_CLASS_PATTERN = pattern;
        WIKIBRAIN_CLASS_BLACKLIST = blacklist;
    }

    /**
     * Returns the first class in the classpath that has the specified short name.
     * For example, "LocalLink" -&gt; org.wikibrain.core.LocalLink.class
     *
     * The full mapping between short names and full names is cached because it is costly to build it.
     *
     * @param shortName
     * @return Class, or null if no full classname matches the short name.
     */
    public static Class classForShortName(String shortName) {
        String fullName = getFullClassName(shortName);
        if (fullName == null) {
            return null;
        }
        try {
            return Class.forName(fullName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);  // class came from the classpath, so this shouldn't happen
        }
    }
    private static Map<String, String> NAME_TO_CLASS = null;

    /**
     * Returns the first element in the classpath that has the specified short name.
     * For example, "LocalLink" -&gt; "org.wikibrain.core.LocalLink"
     * The full mapping between short names and full names is cached because it is costly to build it.
     *
     * @param shortName
     * @return Full name, or null if no full name matches the short name.
     */
    public synchronized static String getFullClassName(String shortName) {
        if (NAME_TO_CLASS != null) {
            return NAME_TO_CLASS.get(shortName);
        }
        NAME_TO_CLASS = new HashMap<String, String>();
        for (File file : getClassPathAsList()) {
            if (file.length() > MAX_FILE_SIZE) {
                LOG.debug("skipping looking for providers in large file " + file);
                continue;
            }
            ClassFinder finder = new ClassFinder();
            finder.add(file);
            ClassFilter filter = new AndClassFilter(
                    new RegexClassFilter(WIKIBRAIN_CLASS_PATTERN.pattern()),
                    new NotClassFilter(new RegexClassFilter(WIKIBRAIN_CLASS_BLACKLIST.pattern()))
            );
            Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
            finder.findClasses(foundClasses,filter);
            for (ClassInfo info : foundClasses) {
                String tokens[] = info.getClassName().split("[.]");
                if (tokens.length == 0) {
                    continue;   // SHOULD NEVER HAPPEN
                }
                String n = tokens[tokens.length - 1];
                if (!NAME_TO_CLASS.containsKey(n)) {
                    NAME_TO_CLASS.put(n, info.getClassName());
                }
            }
        }
        LOG.info("found " + NAME_TO_CLASS.size() + " classes when constructing short to full class name mapping");
//        System.err.println(NAME_TO_CLASS);
        return NAME_TO_CLASS.get(shortName);
    }
}
