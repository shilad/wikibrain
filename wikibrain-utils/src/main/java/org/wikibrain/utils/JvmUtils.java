package org.wikibrain.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class JvmUtils {
    private static final Logger LOG = Logger.getLogger(JvmUtils.class.getName());

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
                            LOG.warning("Illegal url: " + url);
                        }
                    }
                }
            }
            loader = loader.getParent();
        }
        return classPath;
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
        JavaProcessBuilder builder = new JavaProcessBuilder();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        for (String jvmArg : runtimeMxBean.getInputArguments()) {
            if (!jvmArg.startsWith("-")) {
                break;
            }
            if (!jvmArg.equals("-jar")) {
                builder.jvmArg(jvmArg);
            }
        }
        builder.classpath(getClassPath());
        for (String arg : args) {
            builder.arg(arg);
        }
        builder.mainClass(klass.getName());
        return builder.launch(System.out, System.err);
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
}
