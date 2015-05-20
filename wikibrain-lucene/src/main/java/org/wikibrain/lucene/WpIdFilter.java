package org.wikibrain.lucene;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.DocIdBitSet;

import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A lucene filter that only includes a specific set of Wikipedia ids.
 * The constructor is EXPENSIVE, so they should be reused.
 * TODO: Perform a search when there are relatively few wpIds.
 */
public class WpIdFilter extends Filter {
    private static final Logger LOG = LoggerFactory.getLogger(WpIdFilter.class);
    private int[] wpIds;
    private Map<AtomicReader, int[]> allowedLuceneIds = new HashMap<AtomicReader, int[]>();

    public WpIdFilter(int wpIds[]) throws IOException {
        this.wpIds = wpIds;
    }

    @Override
    public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
        BitSet bits = new BitSet();
        int i = 0;
        for (int id : getAllowedLuceneIds(context)) {
            if (acceptDocs == null || acceptDocs.get(id)) {
                bits.set(id);
                i++;
            }
        }
        int n = 0;
        for (int id = 0; id < bits.length(); id++) {
            if (bits.get(id)) n++;
        }
//        LOG.info("bit size=" + bits.size() + " set=" + n + " compared to " + luceneIds.length);
        return new DocIdBitSet(bits);
    }

    private synchronized int[] getAllowedLuceneIds(AtomicReaderContext context) throws IOException {
        AtomicReader reader = context.reader();
        if (allowedLuceneIds.containsKey(reader)) {
            return allowedLuceneIds.get(reader);
        }
        LOG.debug("building WpId filter for " + wpIds.length + " ids with hash " + Arrays.hashCode(wpIds));
        TIntSet wpIdSet = new TIntHashSet(wpIds);
        TIntSet luceneIdSet = new TIntHashSet();
        Set<String> fields = new HashSet<String>(Arrays.asList(LuceneOptions.LOCAL_ID_FIELD_NAME));
        for (int i = 0; i < reader.numDocs(); i++) {
            Document d = reader.document(i, fields);
            int wpId = Integer.valueOf(d.get(LuceneOptions.LOCAL_ID_FIELD_NAME));
            if (wpIdSet.contains(wpId)) {
                luceneIdSet.add(i);
            }
        }
        int luceneIds[] = luceneIdSet.toArray();
        LOG.debug("WpId filter matched " + luceneIds.length + " ids.");
        allowedLuceneIds.put(reader, luceneIds);
        return luceneIds;
    }
}
