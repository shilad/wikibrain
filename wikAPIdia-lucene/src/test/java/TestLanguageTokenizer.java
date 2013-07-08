import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.WikapidiaAnalyzer;

import java.io.IOException;
import java.util.List;

/**
 */
public class TestLanguageTokenizer {

    private Field textField = new TextField("test", "wrap around the world", Field.Store.YES);

    @Test
    public void shortTest() throws IOException, WikapidiaException {
        LuceneOptions opts = LuceneOptions.getDefaultOptions();
        WikapidiaAnalyzer wa = new WikapidiaAnalyzer(Language.getByLangCode("en"));
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
        List<String> langCodes = new Configuration().get().getStringList("languages");
        langCodes.add("he");
        langCodes.add("sk");
        LanguageSet langSet = new LanguageSet(langCodes);
        for(Language language : langSet){
            WikapidiaAnalyzer wa = new WikapidiaAnalyzer(language, opts);
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
