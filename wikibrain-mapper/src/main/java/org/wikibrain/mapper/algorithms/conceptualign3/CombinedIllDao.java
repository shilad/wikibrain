package org.wikibrain.mapper.algorithms.conceptualign3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.UniversalPage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bjhecht on 4/25/14.
 */
public class CombinedIllDao implements InterLanguageLinkDao {

    private final TObjectIntHashMap<LocalId> localId2ItemIdIndex;
    private final Multimap<Integer, LocalId> itemId2LocalIdIndex;
    private final InterLanguageLinkDao illDao;

    public CombinedIllDao(Iterator<UniversalPage> uPages, InterLanguageLinkDao illDao){

        localId2ItemIdIndex = new TObjectIntHashMap<LocalId>();
        itemId2LocalIdIndex = HashMultimap.create();
        this.illDao = illDao;

        buildIndices(uPages);

    }

    private void buildIndices(Iterator<UniversalPage> uPages){

        while(uPages.hasNext()){

            UniversalPage uPage = uPages.next();
            for(LocalId localId : uPage.getLocalEntities()){
                localId2ItemIdIndex.put(localId, uPage.getUnivId());
                itemId2LocalIdIndex.put(uPage.getUnivId(), localId);
            }
        }
    }

    @Override
    public Set<LocalId> getFromSource(Language sourceLang, int sourceId) throws DaoException {

        LocalId localId = new LocalId(sourceLang, sourceId);
        return getFromSource(localId);

    }

    private void addWikidataIlls(LocalId input, Set<LocalId> curIlls){


        if (localId2ItemIdIndex.contains(input)) {
            Integer itemId = localId2ItemIdIndex.get(input);
            curIlls.addAll(itemId2LocalIdIndex.get(itemId));
            curIlls.remove(input);
        }

    }

    @Override
    public Set<LocalId> getFromSource(LocalId source) throws DaoException {


        Set<LocalId> rVal = new HashSet<LocalId>();
        addWikidataIlls(source, rVal);
        rVal.addAll(illDao.getFromSource(source));
        return rVal;

    }

    @Override
    public Set<LocalId> getToDest(Language destLang, int destId) throws DaoException {
        return getToDest(new LocalId(destLang, destId));
    }

    @Override
    public Set<LocalId> getToDest(LocalId dest) throws DaoException {



        Set<LocalId> rVal = new HashSet<LocalId>();
        addWikidataIlls(dest, rVal);
        rVal.addAll(illDao.getToDest(dest));
        return rVal;

    }

    @Override
    public void clear() throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void beginLoad() throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void save(InterLanguageLink item) throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public void endLoad() throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public Iterable<InterLanguageLink> get(DaoFilter daoFilter) throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        throw new RuntimeException("Method not supported");
    }

    @Override
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new RuntimeException("Method not supported");
    }
}
