import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.wikibrain.conf.Configuration;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.lucene.LuceneOptions;
import org.wikibrain.lucene.WikiBrainAnalyzer;

import java.io.IOException;
import java.util.List;

/**
 */
public class TestLanguageTokenizer {

    private Field textField = new TextField("test", "wrap around the world", Field.Store.YES);

    @Test
    public void shortTest() throws IOException, WikiBrainException {
        LuceneOptions opts = LuceneOptions.getDefaultOptions();
        WikiBrainAnalyzer wa = new WikiBrainAnalyzer(Language.getByLangCode("en"));
        IndexWriterConfig iwc = new IndexWriterConfig(opts.matchVersion, wa);
        iwc.setRAMBufferSizeMB(1024.0);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(new RAMDirectory(), iwc);
        Document d = new Document();
        d.add(textField);
        writer.addDocument(d);
        writer.close();
    }

    @Test
    public void test() throws IOException {
        LuceneOptions opts = LuceneOptions.getDefaultOptions();
        List<String> langCodes = new Configuration().get().getStringList("languages.big-economies.langCodes");
        langCodes.add("he");
        langCodes.add("sk");
        LanguageSet langSet = new LanguageSet(langCodes);
        for(Language language : langSet){
            WikiBrainAnalyzer wa = new WikiBrainAnalyzer(language, opts);
            IndexWriterConfig iwc = new IndexWriterConfig(opts.matchVersion, wa);
            iwc.setRAMBufferSizeMB(1024.0);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(new RAMDirectory(), iwc);
            Document d = new Document();
            d.add(textField);
            writer.addDocument(d);
            writer.close();
        }
    }
}
