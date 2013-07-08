import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.TokenizerOptions;
import org.wikapidia.lucene.WikapidiaAnalyzer;

import java.util.List;

/**
 */
public class TestLanguageTokenizer {

    @Test
    public void test() {
        try{
            Field textField = new TextField("test", "wrap around the world", Field.Store.YES);
            List<String> langCodes = new Configuration().get().getStringList("languages");
            langCodes.add("he");
            langCodes.add("sk");
            LanguageSet langSet = new LanguageSet(langCodes);
            LuceneOptions opts = new LuceneOptions();
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
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
