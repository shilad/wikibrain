package org.wikapidia.integration;

import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;

import java.io.File;

/**
 * @author Shilad Sen
 */
public class TestUtils {
    public static final String INTEGRATION_TEST_CONF = "integration-test.conf";
    public static String[] DEFAULT_ARGS = {
                    "-c", "integration-test.conf",
                    "-l", "simple,la"
            };

    public static String[] getArgs(String ...args) {
        return ArrayUtils.addAll(DEFAULT_ARGS, args);
    }

    public static Env getEnv() throws ConfigurationException {
        return new EnvBuilder()
                .setConfigFile(INTEGRATION_TEST_CONF)
                .setLanguages("simple,la")
                .build();
    }

    public static TestDB getTestDb() throws ConfigurationException {
        return new TestDB(getEnv());
    }
}
