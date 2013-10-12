package org.wikapidia.integration;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.download.FileDownloader;
import org.wikapidia.download.RequestedLinkGetter;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author Shilad Sen
 */
public class PipelineIT {
    public static String[] DEFAULT_ARGS = {
                            "-c", "integration-test.conf",
                            "-l", "simple,la"
                        };

    public static String[] getArgs(String ...args) {
        return ArrayUtils.addAll(DEFAULT_ARGS, args);
    }

    @Test
    public void testDownload() throws InterruptedException, WikapidiaException, ConfigurationException, IOException, ParseException {
        RequestedLinkGetter.main(getArgs());
        FileDownloader.main(getArgs());
    }
}
