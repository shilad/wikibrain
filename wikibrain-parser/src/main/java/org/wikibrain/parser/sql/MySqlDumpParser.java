package org.wikibrain.parser.sql;

import com.akiban.sql.StandardException;
import com.akiban.sql.parser.*;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a MySQL dump file containing insert statements.
 */
public class MySqlDumpParser {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlDumpParser.class);

    private SQLParser sqlParser = new SQLParser();

    /**
     * Parses a mysql dump into rows.
     * @param dump The file containing the schema and insert statements
     * @return An iterable of maps, each containing column name to column value.
     */
    public Iterable<Object[]> parse(final File dump) {
        return new Iterable<Object[]>() {
            @Override
            public Iterator<Object[]> iterator() {
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

    /**
     * Parses a mysql dump into rows.
     * @param line The line containing (possibly multiple) insert statements.
     * @return An iterable of maps, each containing column name to column value.
     */
    public List<Object[]> parse(String line) throws StandardException {
        final List<Object[]> result = new ArrayList<Object[]>();
        if (!line.startsWith("INSERT ")) {
            return result;
        }
        line = unescapeString(line);
        if (line.endsWith(";")) {
            line = line.substring(0, line.length()-1);
        }

        StatementNode node = sqlParser.parseStatement(line);
        if (node instanceof InsertNode) {
            node.accept(new Visitor() {
                @Override
                public Visitable visit(Visitable node) throws StandardException {
                    if (node instanceof RowResultSetNode) {
                        List<Object> values = new ArrayList<Object>();
                        for (ResultColumn column : ((RowResultSetNode)node).getResultColumns()) {
                            // TODO: are other types of values possible?
                            ConstantNode value = (ConstantNode) column.getExpression();
                            values.add(value.getValue());
                        }
                        result.add(values.toArray());
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
                    return (node instanceof RowResultSetNode);
                }
            });
        }
        return result;
    }

    /**
     * A buffered iterator for a file containing insert statements.
     */
    class MyIterator implements Iterator<Object[]> {
        private final File path;
        private List<Object[]> buffer = new LinkedList<Object[]>();
        private BufferedReader reader;
        private int line = 0;

        public MyIterator(File path) throws IOException {
            this.path = path;
            reader = WpIOUtils.openBufferedReader(path);
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
                    LOG.error("error parsing line "  + line + " of " + path + ":", e);
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
        public Object[] next() {
            fillBuffer();
            return buffer.remove(0);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
