package org.wikibrain.loader.pipeline;

import org.parse4j.ParseException;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.utils.WpIOUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class LoadTimeEstimator {
    private final Env env;
    private final List<StageDiagnostic> diagnostics;

    public LoadTimeEstimator(Env env) throws ConfigurationException, ParseException {
        this.env = env;
        this.diagnostics = env.getConfigurator().get(DiagnosticDao.class).getAll();
        System.out.println("read " + this.diagnostics.size() + " diagnostics");
    }

    public void writeAllData(String path) throws IOException {
        CsvListWriter writer = new CsvListWriter(WpIOUtils.openWriter(path), CsvPreference.STANDARD_PREFERENCE);
        writer.write(Arrays.asList("stage", "singleCoreSpeed", "multiCoreSpeed", "numLinks", "numArticles", "elapsed"));
        for (StageDiagnostic diagnostic : diagnostics) {
            int numArticles = 0;
            int numLinks = 0;
            for (Language l : diagnostic.getLangs()) {
                numLinks += LanguageInfo.getByLanguage(l).getNumLinks();
                numArticles += LanguageInfo.getByLanguage(l).getNumArticles();
            }

            writer.write(Arrays.asList(
                    diagnostic.getStage(),
                    diagnostic.getSingleCoreSpeed(),
                    diagnostic.getMultiCoreSpeed(),
                    numLinks,
                    numArticles,
                    diagnostic.getElapsedSeconds()
                ));
        }

        writer.close();
    }



    public static void main(String args[]) throws ConfigurationException, ParseException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        LoadTimeEstimator fitter = new LoadTimeEstimator(env);
        fitter.writeAllData("timings.csv");
    }
}
