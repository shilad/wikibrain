package org.wikapidia.dao.load;

import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.download.FileDownloader;
import org.wikapidia.download.RequestedLinkGetter;
import org.wikapidia.utils.JvmUtils;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class PipelineLoader {
    public static void main(String args[]) throws IOException, InterruptedException {
        run(RequestedLinkGetter.class, args);
        run(FileDownloader.class, args);
        run(DumpLoader.class, ArrayUtils.addAll(args, "-d"));
        run(RedirectLoader.class, ArrayUtils.add(args, "-d"));
        run(WikiTextLoader.class, ArrayUtils.add(args, "-d"));
        run(LuceneLoader.class, args);
        run(ConceptLoader.class, ArrayUtils.add(args, "-d"));
        run(UniversalLinkLoader.class, ArrayUtils.add(args, "-d"));
        run(PhraseLoader.class, ArrayUtils.addAll(args, "-p", "anchortext"));
    }

    public static void run(Class klass, String args[]) throws IOException, InterruptedException {
        Process p = JvmUtils.launch(klass, args);
        int retVal = p.waitFor();
        if (retVal != 0) {
            System.err.println("command failed with exit code " + retVal + " : ");
            System.err.println("ABORTING!");
            System.exit(retVal);
        }
    }
}
