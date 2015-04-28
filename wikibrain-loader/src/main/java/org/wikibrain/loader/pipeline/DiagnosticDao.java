package org.wikibrain.loader.pipeline;

import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.parse4j.Parse;
import org.parse4j.ParseException;
import org.parse4j.ParseObject;
import org.parse4j.ParseQuery;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class DiagnosticDao {
    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticDao.class);
    private final String token;
    private final File logFile;

    public DiagnosticDao(String appId, String restApiId, String token, File logFile) {
        Parse.initialize(appId, restApiId);
        this.logFile = logFile;
        this.token = token;
        logFile.getParentFile().mkdirs();
    }

    public void save(StageDiagnostic diagnostic) throws ParseException, IOException {
        String contents = "";
        if (!logFile.exists()) {
            contents = "stage\tdate\tlangs\telapsed\tsingleCoreSpeed\tmultiCoreSpeed\tmegabytes\tsucceeded\n";
        }
        contents += StringUtils.join(Arrays.asList(
                            diagnostic.getStage(),
                            diagnostic.getDate().toString(),
                            diagnostic.getLangs().getLangCodeString(),
                            diagnostic.getElapsedSeconds(),
                            diagnostic.getSingleCoreSpeed(),
                            diagnostic.getMultiCoreSpeed(),
                            diagnostic.getMegabytesUsed(),
                            diagnostic.getSucceeded()
                    ), "\t") + "\n";
        FileUtils.write(logFile, contents, true);
        ParseObject object = new ParseObject("StageDiagnostic");
        object.put("installToken", token);
        object.put("runId", diagnostic.getRunId());
        object.put("system", diagnostic.getSystem());
        object.put("stage", diagnostic.getStage());
        object.put("date", diagnostic.getDate());
        object.put("langs", diagnostic.getLangs().getLangCodeString());
        object.put("elapsed", diagnostic.getElapsedSeconds());
        object.put("singleCoreSpeed", diagnostic.getSingleCoreSpeed());
        object.put("multiCoreSpeed", diagnostic.getMultiCoreSpeed());
        object.put("megabytes", diagnostic.getMegabytesUsed());
        object.put("suceeded", diagnostic.getSucceeded());
        object.put("version", "0.4-SNAPSHOT");
        object.save();
    }

    public void saveQuietly(StageDiagnostic diagnostic) {
        try {
            save(diagnostic);
        } catch (Exception e) {
            LOG.warn("Save of diagnostics failed: ", e);
        }
    }

    public List<StageDiagnostic> getAll() throws ParseException {
        List<StageDiagnostic> result = new ArrayList<StageDiagnostic>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("StageDiagnostic");
        for (ParseObject object : query.find()) {
            result.add(new StageDiagnostic(
                    object.getLong("runId"),
                    object.getString("stage"),
                    new LanguageSet(object.getString("langs")),
                    object.getDouble("elapsed"),
                    object.getDouble("singleCoreSpeed"),
                    object.getDouble("multiCoreSpeed"),
                    object.getDouble("megabytes")
            ));
        }
        return result;
    }

    public static class Provider extends org.wikibrain.conf.Provider<DiagnosticDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<DiagnosticDao> getType() {
            return DiagnosticDao.class;
        }

        @Override
        public String getPath() {
            return "dao.diagnostic";
        }

        @Override
        public DiagnosticDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            try {
                File tokenFile = new File(config.getString("tokenFile"));
                if (!tokenFile.isFile()) {
                    Random random = new Random();
                        FileUtils.write(tokenFile, "" + Math.abs(random.nextLong()));
                }
                String token = FileUtils.readFileToString(tokenFile).trim();
                String appId = config.getString("appId");
                String restApiId = config.getString("restApiId");
                File logFile = new File(config.getString("log"));
                return new DiagnosticDao(appId, restApiId, token, logFile);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
