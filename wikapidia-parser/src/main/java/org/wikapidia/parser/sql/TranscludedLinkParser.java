package org.wikapidia.parser.sql;

import com.akiban.sql.StandardException;
import com.akiban.sql.parser.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.utils.CompressedFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses a SQL file containing transcluded links
 */
public class TranscludedLinkParser {
    private static final Logger LOG = Logger.getLogger(TranscludedLinkParser.class.getName());

    private SQLParser sqlParser = new SQLParser();
    public static class RawLink {
        public int srcPageId;
        public int destNamespace;
        public String destTitle;
    }


    public Iterable<RawLink> parse(final File dump) {
        return new Iterable<RawLink>() {
            @Override
            public Iterator<RawLink> iterator() {
                try {
                    return new MyIterator(dump);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * See: http://dev.mysql.com/doc/refman/5.0/en/string-literals.html
     * @param line
     * @return
     */
    protected String unescapeString(String line) {
        StringBuffer result = new StringBuffer();
        int i = 0;
        while (true) {
            int j = line.indexOf("\\", i);
            if (j < 0) {
                break;
            } else if (j == line.length() - 1) { // how to handle a trailing slash?
                break;
            } else {
                result.append(line.substring(i, j));
                String escaped;
                switch (line.charAt(j+1)) {
                    case '0' :  escaped = "\0"; break;
                    case '\'' : escaped = "''"; break;
                    case '"' :  escaped = "\""; break;
                    case 'b' :  escaped = "\b"; break;
                    case 'n' :  escaped = "\n"; break;
                    case 'r' :  escaped = "\r"; break;
                    case 't' :  escaped = "\t"; break;
                    case 'Z' :  escaped = "\u001a"; break;
                    case '\\' : escaped = "\\"; break;
                    case '_' :  escaped = "_'"; break;
                    default:
                        throw new IllegalArgumentException("invalid escape character encountered: " + line.charAt(j+1));
                }
                result.append(escaped);
                i = j + 2;
            }
        }
        result.append(line.substring(i));
        return result.toString();
    }

    public List<RawLink> parse(String line) throws StandardException {
        final List<RawLink> result = new ArrayList<RawLink>();
        if (!line.startsWith("INSERT ")) {
            return result;
        }
        line = unescapeString(line);
        if (line.endsWith(";")) {
            line = line.substring(0, line.length()-1);
        }

        try {
            FileUtils.write(new File("/tmp/line.txt"), line, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        StatementNode node = sqlParser.parseStatement(line);
        if (node instanceof InsertNode) {
            node.accept(new Visitor() {
                @Override
                public Visitable visit(Visitable node) throws StandardException {
                    if (node instanceof RowResultSetNode) {
                        ResultColumnList row = ((RowResultSetNode)node).getResultColumns();
                        Integer i = (Integer) ((NumericConstantNode)row.getResultColumn(1).getExpression()).getValue();
                        Integer j = (Integer) ((NumericConstantNode)row.getResultColumn(2).getExpression()).getValue();
                        String k = (String) ((CharConstantNode)row.getResultColumn(3).getExpression()).getValue();
                        RawLink rl = new RawLink();
                        rl.srcPageId = i;
                        rl.destNamespace = j;
                        rl.destTitle = k;
                        result.add(rl);
                    }
                    return node;
                }

                @Override
                public boolean visitChildrenFirst(Visitable node) {
                    return true;
                }

                @Override
                public boolean stopTraversal() {
                    return false;
                }

                @Override
                public boolean skipChildren(Visitable node) throws StandardException {
                    return false;
                }
            });
        }
        return result;
    }

    class MyIterator implements Iterator<RawLink> {
        private final File path;
        private List<RawLink> buffer = new LinkedList<RawLink>();
        private BufferedReader reader;
        private int line = 0;

        public MyIterator(File path) throws IOException {
            this.path = path;
            reader = CompressedFile.open(path);
        }

        private void fillBuffer() {
            if (buffer.size() > 0) {
                return;
            }
            if (reader == null) {
                return;
            }
            while (buffer.isEmpty()) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        reader = null;
                        return;
                    }
                    buffer.addAll(parse(line));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (StandardException e) {
                    LOG.log(Level.SEVERE, "error parsing line "  + line + " of " + path + ":", e);
                    System.exit(1);
                }
                line++;
            }
        }
        @Override
        public boolean hasNext() {
            fillBuffer();
            return buffer.size() > 0;
        }

        @Override
        public RawLink next() {
            fillBuffer();
            return buffer.remove(0);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
