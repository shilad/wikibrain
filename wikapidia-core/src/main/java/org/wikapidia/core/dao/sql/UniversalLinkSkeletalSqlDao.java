package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.UniversalLink;
import org.wikapidia.core.model.UniversalLinkGroup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Ari Weiland
 */
public class UniversalLinkSkeletalSqlDao extends AbstractSqlDao<UniversalLink> implements UniversalLinkDao {

    public UniversalLinkSkeletalSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-skeletal-link");
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_1,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_2,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_3,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_4,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_5,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_6,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_7,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_8,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_9,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_10,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_11,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_12,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_13,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_14,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_15,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_16,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_17,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_18,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_19,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_20,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_21,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_22,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_23,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_24,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_25,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_26,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_27,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_28,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_29,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_30,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_31,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_32,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_33,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_34,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_35,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_36,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_37,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_38,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_39,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_40,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_41,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_42,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_43,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_44,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_45,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_46,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_47,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_48,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_49,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_50,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_51,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_52,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_53,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_54,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_55,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_56,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_57,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_58,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_59,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_60,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_61,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_62,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_63,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_64,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_65,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_66,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_67,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_68,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_69,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_70,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_71,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_72,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_73,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_74,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_75,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_76,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_77,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_78,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_79,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_80,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_81,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_82,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_83,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_84,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_85,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_86,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_87,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_88,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_89,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_90,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_91,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_92,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_93,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_94,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_95,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_96,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_97,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_98,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_99,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_100,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_101,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_102,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_103,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_104,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_105,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_106,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_107,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_108,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_109,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_110,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_111,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_112,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_113,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_114,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_115,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_116,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_117,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_118,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_119,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_120,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_121,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_122,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_123,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_124,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_125,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_126,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_127,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_128,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_129,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_130,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_131,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_132,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_133,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_134,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_135,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_136,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_137,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_138,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_139,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_140,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_141,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_142,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_143,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_144,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_145,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_146,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_147,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_148,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_149,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_150,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_151,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_152,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_153,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_154,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_155,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_156,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_157,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_158,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_159,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_160,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_161,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_162,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_163,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_164,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_165,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_166,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_167,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_168,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_169,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_170,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_171,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_172,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_173,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_174,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_175,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_176,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_177,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_178,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_179,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_180,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_181,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_182,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_183,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_184,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_185,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_186,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_187,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_188,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_189,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_190,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_191,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_192,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_193,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_194,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_195,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_196,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_197,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_198,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_199,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_200,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_201,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_202,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_203,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_204,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_205,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_206,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_207,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_208,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_209,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_210,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_211,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_212,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_213,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_214,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_215,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_216,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_217,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_218,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_219,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_220,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_221,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_222,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_223,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_224,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_225,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_226,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_227,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_228,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_229,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_230,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_231,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_232,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_233,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_234,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_235,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_236,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_237,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_238,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_239,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_240,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_241,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_242,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_243,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_244,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_245,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_246,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_247,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_248,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_249,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_250,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_251,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_252,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_253,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_254,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_255,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_256,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_257,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_258,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_259,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_260,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_261,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_262,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_263,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_264,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_265,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_266,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_267,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_268,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_269,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_270,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_271,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_272,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_273,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_274,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_275,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_276,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_277,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_278,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_279,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_280,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_281,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_282,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_283,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_284,
            Tables.UNIVERSAL_SKELETAL_LINK.LANG_285
    };

    @Override
    public void save(UniversalLink item) throws DaoException {
        Collection<Language> temp = item.getLanguageSetOfExistsInLangs().getLanguages();
        Set<Short> langIds = Sets.newHashSet();
        for (Language l : temp) {
            langIds.add(l.getId());
        }
        Object[] toInsert = new Object[288];
        toInsert[0] = item.getSourceUnivId();
        toInsert[1] = item.getDestUnivId();
        toInsert[2] = item.getAlgorithmId();
        for (int i=3; i<toInsert.length; i++) {
            toInsert[i] = langIds.contains((short) (i - 2));
        }
        insert(toInsert);
    }

    @Override
    public Iterable<UniversalLink> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
            }
            // for language, it will only return universal links that are in EVERY specified language
            if (daoFilter.getLangIds() != null) {
                for (short langId : daoFilter.getLangIds()) {
                    conditions.add(getLangField(langId).eq(true));
                }
            }
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(conditions)
                    .fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<UniversalLink>(result, conn) {

                @Override
                public UniversalLink transform(Record item) throws DaoException {
                    return buildUniversalLink(item);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
        }
    }

    @Override
    public UniversalLinkGroup getOutlinks(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public UniversalLinkGroup getInlinks(int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, false);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public UniversalLink getUniversalLink(int sourceId, int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetchOne();
            return buildUniversalLink(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }
    private UniversalLinkGroup buildUniversalLinkGroup(Result<Record> result, boolean outlinks) throws DaoException {
        if (result == null || result.isEmpty()) {
            return null;
        }
        Map<Integer, UniversalLink> map = new HashMap<Integer, UniversalLink>();
        int commonId = -1;
        int algorithmId = -1;
        for (Record record : result) {
            map.put(
                    record.getValue(outlinks ?                          // Gets the unique ID of the links
                            Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID :    // If links are outlinks, dest ID is unique
                            Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID),  // If links are inlinks, source ID is unique
                    buildUniversalLink(record));
            if (commonId == -1) {
                commonId = record.getValue(outlinks ?               // Gets the common ID of the links
                        Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID :  // If links are outlinks, source ID is common
                        Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID);    // If links are inlinks, dest ID is common;
                algorithmId = record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID);
            }
        }
        Set<Language> languages = new HashSet<Language>();
        for (UniversalLink link : map.values()) {
            for (Language language : link.getLanguageSetOfExistsInLangs()) {
                languages.add(language);
            }
        }
        return new UniversalLinkGroup(
                map,
                outlinks,
                commonId,
                algorithmId,
                new LanguageSet(languages)
        );
    }

    private UniversalLink buildUniversalLink(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        List<Language> languages = new ArrayList<Language>();
        for (Language language : LanguageSet.ALL) {
            if (record.getValue(getLangField(language))) {
                languages.add(language);
            }
        }
        return new UniversalLink(
                record.getValue(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID),
                record.getValue(Tables.UNIVERSAL_LINK.UNIV_DEST_ID),
                record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID),
                new LanguageSet(languages)
        );
    }

    private TableField<Record, Boolean> getLangField(Language language) {
        return getLangField(language.getId());
    }

    private TableField<Record, Boolean> getLangField(short langId) {
        return INSERT_FIELDS[langId + 2];
    }
}
