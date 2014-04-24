package org.wikibrain.dao.load;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.wiki.ParsedIll;
import org.wikibrain.parser.wiki.ParserVisitor;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shilad Sen
 */
public class InterLanguageLinkExtractor {

    static class IllParserVisitor extends ParserVisitor {
        private AtomicInteger count = new AtomicInteger();
        private BufferedWriter output;

        public IllParserVisitor(BufferedWriter output) {
            this.output = output;
        }

        public void ill(ParsedIll ill) throws WikiBrainException {
            RawPage page = ill.location.getXml();
            try {
                // This format may not be easy to parse. Change it.
                synchronized (output) {
                    this.output.write(
                            page.getLanguage().getLangCode() + "\t" + page.getTitle().getCanonicalTitle() + "\t" +
                            ill.title.getLanguage().getLangCode() + "\t" + ill.title.getCanonicalTitle() + "\n");
                }
                count.incrementAndGet();
            } catch (IOException e) {
                throw new WikiBrainException(e);
            }
        }

        public int getCount() {
            return count.get();
        }
    }


    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        LanguageSet langs = env.getConfigurator().get(LanguageSet.class);
        RawPageDao dao = env.getConfigurator().get(RawPageDao.class);
        BufferedWriter output = WpIOUtils.openWriter(new File("ills.txt"));
        ParserVisitor visitor = new IllParserVisitor(output);
        WikiTextLoader.maxThreadsPerLang = env.getMaxThreads(); // HACK
        for (Language lang : langs) {
            System.out.println("extracting ills for: " + lang);
            WikiTextLoader loader = new WikiTextLoader(Arrays.asList(visitor), LanguageSet.ALL, dao, env.getMaxThreads());
            loader.load(LanguageInfo.getByLanguage(lang));
        }
        System.out.println("extracted " + ((IllParserVisitor)visitor).getCount() + " ills");
        output.close();
    }
}
