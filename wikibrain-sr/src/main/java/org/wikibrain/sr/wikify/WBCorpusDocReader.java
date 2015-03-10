package org.wikibrain.sr.wikify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.wikibrain.sr.wikify.WbCorpusLineReader.*;

/**
 * @author Shilad Sen
 */
public class WBCorpusDocReader implements Iterable<WBCorpusDocReader.Doc> {

    private final File path;

    public WBCorpusDocReader(File path) {
        this.path = path;
    }

    @Override
    public Iterator<Doc> iterator() {
        final Iterator<Line> delegate;
        try {
             delegate = new WBCorpusLineIterator(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error creating delegate for " + path + ": ", e);
        }

        return new Iterator<Doc>() {
            Doc accum = null;

            @Override
            public synchronized boolean hasNext() {
                return accum != null || delegate.hasNext();
            }

            @Override
            public synchronized Doc next() {
                Doc result = null;
                while (result == null) {
                    Line l = delegate.next();
                    if (l == null) {
                        result = accum;
                        accum = null;
                    } else if (accum == null) {
                        accum = new Doc(l);
                    } else if (!accum.getDoc().equals(l.getDoc())) {
                        result = accum;
                        accum = new Doc(l);
                    } else {
                        accum.addLine(l);
                    }
                }
                return result;
            }

            @Override
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }


    public static class Doc {
        private final CorpusInfo corpus;
        private final DocInfo doc;
        private final List<String> lines = new ArrayList<String>();

        public Doc(Line line) {
            this.corpus = line.getCorpus();
            this.doc = line.getDoc();
            this.lines.add(line.getLine());
        }

        public void addLine(Line line) {
            this.lines.add(line.getLine());
        }

        public CorpusInfo getCorpus() {
            return corpus;
        }

        public DocInfo getDoc() {
            return doc;
        }

        public List<String> getLines() {
            return lines;
        }
    }
}
