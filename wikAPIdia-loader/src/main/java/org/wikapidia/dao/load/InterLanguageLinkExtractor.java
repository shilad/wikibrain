package org.wikapidia.dao.load;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.ParsedIll;
import org.wikapidia.parser.wiki.ParserVisitor;

import java.io.BufferedWriter;
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

        public void ill(ParsedIll ill) throws WikapidiaException {
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
                throw new WikapidiaException(e);
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
        BufferedWriter output = new BufferedWriter(new FileWriter("ills.txt"));
        ParserVisitor visitor = new IllParserVisitor(output);
        for (Language lang : langs) {
            System.out.println("extracting ills for: " + lang);
            WikiTextLoader loader = new WikiTextLoader(Arrays.asList(visitor), LanguageSet.ALL, dao, env.getMaxThreads());
            loader.load(LanguageInfo.getByLanguage(lang));
        }
        System.out.println("extracted " + ((IllParserVisitor)visitor).getCount() + " ills");
        output.close();
    }
}
