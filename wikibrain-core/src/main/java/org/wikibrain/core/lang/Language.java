package org.wikibrain.core.lang;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A language associated with a language edition of Wikipedia.
 * The set of all Languages is loaded when the system starts up from a text file.
 * Languages can be queried by langCode or id.
 */
public class Language implements Comparable<Language>, Serializable {
    private static final long serialVersionUID = 6331325313592646604l;

    public static final String LANGUAGE_TSV = "languages.tsv";

    /**
     * Languages is immediately initialized based on the languages.tsv.
     */
    public static Language[] LANGUAGES;

    static {
        InputStream stream = null;
        try {
            stream = Language.class.getClassLoader()
                    .getResourceAsStream(LANGUAGE_TSV);
            loadAllLanguages(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);  // What else can we do?
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }


    private final short id;
    private final String langCode;
    private final String enLangName;
    private final String nativeName;
    private Locale locale;

    private Language(short id, String langCode, String enLangName, String nativeName) {
        this.id = id;
        this.langCode = langCode;
        this.enLangName = enLangName;
        this.nativeName = nativeName;
    }

    public short getId() {
        return id;
    }

    public String getLangCode() {
        return langCode;
    }

    public String getEnLangName() {
        return enLangName;
    }

    public String getNativeName() {
        return nativeName;
    }

    public Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        synchronized (this) {
            if (locale == null){
                locale = new Locale(langCode);
            }
        }
        return locale;
    }


    /**
     * @param langCode langCode, such as "en"
     * @return associated language
     * @throws IllegalArgumentException if langCode is unknown.
     */
    public static Language getByLangCode(String langCode) {
        langCode = langCode.replace('_', '-').toLowerCase();
        if (WIKIDATA.getLangCode().equals(langCode)) {
            return WIKIDATA;
        }
        for (Language lang : LANGUAGES) {
            if (lang.langCode.equalsIgnoreCase(langCode)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("unknown langCode: '" + langCode + "'");
    }

    public static Language getByLangCodeLenient(String langCode) {
        langCode = langCode.replace('_', '-').toLowerCase();
        List<String> flavors = new ArrayList<String>();
        flavors.add(langCode);
        if (langCode.contains("-")) {
            flavors.add(langCode.substring(0, langCode.indexOf("-")));
        }
        for (String s : flavors) {
            try {
                return getByLangCode(s);
            } catch (IllegalArgumentException e) {
            }
        }
        throw new IllegalArgumentException("unknown langCode: '" + langCode + "'");
    }

    public static Language getByFullLangName(String language) {
        for (Language lang : LANGUAGES) {
            if (lang.enLangName.equalsIgnoreCase(language) || lang.nativeName.equalsIgnoreCase(language)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("unknown language: '" + language + "'");

    }

    public static boolean hasLangCode(String langCode) {
        langCode = langCode.replace('_', '-').toLowerCase();
        for (Language lang : LANGUAGES) {
            if (lang.langCode.equalsIgnoreCase(langCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param id numeric id associated with the language
     * @return associated language                                             w
     * @throws IllegalArgumentException if id is unknown.
     */
    public static Language getById(int id) {
        if (0 < id && id <= LANGUAGES.length) {
            return LANGUAGES[id-1];
        } else {
            throw new IllegalArgumentException("unknown language id: '" + id + "'");
        }
    }

    /**
     * Loads the languages from the text file.
     */
    static private void loadAllLanguages(InputStream stream) throws IOException {
        List<String> lines = IOUtils.readLines(stream, "UTF-8");
        String header = lines.get(0);
        lines = lines.subList(1, lines.size());
        if (header.equals("id\tlangCode\tenLangName\tnativeName")) {
            LANGUAGES = new Language[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                String[] cols = StringUtils.splitPreserveAllTokens(lines.get(i), "\t");
                short id = Short.parseShort(cols[0]);
                //int id = Integer.parseInt(cols[0]);
                if (id != i+1) {
                    throw new IOException("expected language id " + (i+1) + ", but got " + id);
                }
                LANGUAGES[i] = new Language(id, cols[1], cols[2], cols[3]);
            }
        } else {
            throw new IOException("invalid header in languages.tsv: " + header);
        }
    }

    public String getDomain() {
        return langCode + ".wikipedia.org";
    }

    public LanguageInfo getLanguageInfo() {
        return LanguageInfo.getByLanguage(this);
    }

    @Override
    public int compareTo(Language language) {
        return Short.valueOf(this.id).compareTo(language.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Language language = (Language) o;

        if (id != language.id) return false;
        if (!langCode.equals(language.langCode)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) id;
        result = 31 * result + langCode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getEnLangName();
    }

    /**
     * HACK: Not really a language, but treated as a language by Wikimedia.
     * Must come before other languages.
     */
    public static Language WIKIDATA = new Language((short) -1, "wikidata", "Wikidata", "Wikidata");


    /**
     * These can be automatically regenereated by running
     * mac-wikibrain/wikibrain-core/src/main/resources/make_lang_constants.py
     */
    public static final Language EN = Language.getByLangCode("en");
    public static final Language DE = Language.getByLangCode("de");
    public static final Language FR = Language.getByLangCode("fr");
    public static final Language NL = Language.getByLangCode("nl");
    public static final Language IT = Language.getByLangCode("it");
    public static final Language PL = Language.getByLangCode("pl");
    public static final Language ES = Language.getByLangCode("es");
    public static final Language RU = Language.getByLangCode("ru");
    public static final Language JA = Language.getByLangCode("ja");
    public static final Language PT = Language.getByLangCode("pt");
    public static final Language ZH = Language.getByLangCode("zh");
    public static final Language SV = Language.getByLangCode("sv");
    public static final Language VI = Language.getByLangCode("vi");
    public static final Language UK = Language.getByLangCode("uk");
    public static final Language CA = Language.getByLangCode("ca");
    public static final Language NO = Language.getByLangCode("no");
    public static final Language FI = Language.getByLangCode("fi");
    public static final Language CS = Language.getByLangCode("cs");
    public static final Language HU = Language.getByLangCode("hu");
    public static final Language KO = Language.getByLangCode("ko");
    public static final Language FA = Language.getByLangCode("fa");
    public static final Language ID = Language.getByLangCode("id");
    public static final Language TR = Language.getByLangCode("tr");
    public static final Language AR = Language.getByLangCode("ar");
    public static final Language RO = Language.getByLangCode("ro");
    public static final Language SK = Language.getByLangCode("sk");
    public static final Language EO = Language.getByLangCode("eo");
    public static final Language DA = Language.getByLangCode("da");
    public static final Language SR = Language.getByLangCode("sr");
    public static final Language LT = Language.getByLangCode("lt");
    public static final Language MS = Language.getByLangCode("ms");
    public static final Language HE = Language.getByLangCode("he");
    public static final Language EU = Language.getByLangCode("eu");
    public static final Language SL = Language.getByLangCode("sl");
    public static final Language BG = Language.getByLangCode("bg");
    public static final Language KK = Language.getByLangCode("kk");
    public static final Language VO = Language.getByLangCode("vo");
    public static final Language HR = Language.getByLangCode("hr");
    public static final Language WAR = Language.getByLangCode("war");
    public static final Language HI = Language.getByLangCode("hi");
    public static final Language ET = Language.getByLangCode("et");
    public static final Language GL = Language.getByLangCode("gl");
    public static final Language AZ = Language.getByLangCode("az");
    public static final Language NN = Language.getByLangCode("nn");
    public static final Language SIMPLE = Language.getByLangCode("simple");
    public static final Language LA = Language.getByLangCode("la");
    public static final Language EL = Language.getByLangCode("el");
    public static final Language TH = Language.getByLangCode("th");
    public static final Language NEW = Language.getByLangCode("new");
    public static final Language ROA_RUP = Language.getByLangCode("roa-rup");
    public static final Language OC = Language.getByLangCode("oc");
    public static final Language SH = Language.getByLangCode("sh");
    public static final Language KA = Language.getByLangCode("ka");
    public static final Language MK = Language.getByLangCode("mk");
    public static final Language TL = Language.getByLangCode("tl");
    public static final Language HT = Language.getByLangCode("ht");
    public static final Language PMS = Language.getByLangCode("pms");
    public static final Language TE = Language.getByLangCode("te");
    public static final Language TA = Language.getByLangCode("ta");
    public static final Language BE_X_OLD = Language.getByLangCode("be-x-old");
    public static final Language BE = Language.getByLangCode("be");
    public static final Language BR = Language.getByLangCode("br");
    public static final Language CEB = Language.getByLangCode("ceb");
    public static final Language LV = Language.getByLangCode("lv");
    public static final Language SQ = Language.getByLangCode("sq");
    public static final Language JV = Language.getByLangCode("jv");
    public static final Language MG = Language.getByLangCode("mg");
    public static final Language CY = Language.getByLangCode("cy");
    public static final Language LB = Language.getByLangCode("lb");
    public static final Language MR = Language.getByLangCode("mr");
    public static final Language IS = Language.getByLangCode("is");
    public static final Language BS = Language.getByLangCode("bs");
    public static final Language YO = Language.getByLangCode("yo");
    public static final Language AN = Language.getByLangCode("an");
    public static final Language LMO = Language.getByLangCode("lmo");
    public static final Language HY = Language.getByLangCode("hy");
    public static final Language FY = Language.getByLangCode("fy");
    public static final Language BPY = Language.getByLangCode("bpy");
    public static final Language ML = Language.getByLangCode("ml");
    public static final Language PNB = Language.getByLangCode("pnb");
    public static final Language SW = Language.getByLangCode("sw");
    public static final Language BN = Language.getByLangCode("bn");
    public static final Language IO = Language.getByLangCode("io");
    public static final Language AF = Language.getByLangCode("af");
    public static final Language GU = Language.getByLangCode("gu");
    public static final Language ZH_YUE = Language.getByLangCode("zh-yue");
    public static final Language NE = Language.getByLangCode("ne");
    public static final Language NDS = Language.getByLangCode("nds");
    public static final Language UR = Language.getByLangCode("ur");
    public static final Language KU = Language.getByLangCode("ku");
    public static final Language UZ = Language.getByLangCode("uz");
    public static final Language AST = Language.getByLangCode("ast");
    public static final Language SCN = Language.getByLangCode("scn");
    public static final Language SU = Language.getByLangCode("su");
    public static final Language QU = Language.getByLangCode("qu");
    public static final Language DIQ = Language.getByLangCode("diq");
    public static final Language BA = Language.getByLangCode("ba");
    public static final Language TT = Language.getByLangCode("tt");
    public static final Language MY = Language.getByLangCode("my");
    public static final Language GA = Language.getByLangCode("ga");
    public static final Language CV = Language.getByLangCode("cv");
    public static final Language IA = Language.getByLangCode("ia");
    public static final Language NAP = Language.getByLangCode("nap");
    public static final Language BAT_SMG = Language.getByLangCode("bat-smg");
    public static final Language MAP_BMS = Language.getByLangCode("map-bms");
    public static final Language WA = Language.getByLangCode("wa");
    public static final Language ALS = Language.getByLangCode("als");
    public static final Language KN = Language.getByLangCode("kn");
    public static final Language AM = Language.getByLangCode("am");
    public static final Language GD = Language.getByLangCode("gd");
    public static final Language BUG = Language.getByLangCode("bug");
    public static final Language TG = Language.getByLangCode("tg");
    public static final Language ZH_MIN_NAN = Language.getByLangCode("zh-min-nan");
    public static final Language YI = Language.getByLangCode("yi");
    public static final Language VEC = Language.getByLangCode("vec");
    public static final Language SCO = Language.getByLangCode("sco");
    public static final Language HIF = Language.getByLangCode("hif");
    public static final Language ROA_TARA = Language.getByLangCode("roa-tara");
    public static final Language OS = Language.getByLangCode("os");
    public static final Language ARZ = Language.getByLangCode("arz");
    public static final Language NAH = Language.getByLangCode("nah");
    public static final Language MZN = Language.getByLangCode("mzn");
    public static final Language SAH = Language.getByLangCode("sah");
    public static final Language KY = Language.getByLangCode("ky");
    public static final Language MN = Language.getByLangCode("mn");
    public static final Language SA = Language.getByLangCode("sa");
    public static final Language PAM = Language.getByLangCode("pam");
    public static final Language HSB = Language.getByLangCode("hsb");
    public static final Language LI = Language.getByLangCode("li");
    public static final Language MI = Language.getByLangCode("mi");
    public static final Language SI = Language.getByLangCode("si");
    public static final Language CO = Language.getByLangCode("co");
    public static final Language CKB = Language.getByLangCode("ckb");
    public static final Language GAN = Language.getByLangCode("gan");
    public static final Language GLK = Language.getByLangCode("glk");
    public static final Language BO = Language.getByLangCode("bo");
    public static final Language FO = Language.getByLangCode("fo");
    public static final Language BAR = Language.getByLangCode("bar");
    public static final Language BCL = Language.getByLangCode("bcl");
    public static final Language ILO = Language.getByLangCode("ilo");
    public static final Language MRJ = Language.getByLangCode("mrj");
    public static final Language SE = Language.getByLangCode("se");
    public static final Language FIU_VRO = Language.getByLangCode("fiu-vro");
    public static final Language NDS_NL = Language.getByLangCode("nds-nl");
    public static final Language TK = Language.getByLangCode("tk");
    public static final Language VLS = Language.getByLangCode("vls");
    public static final Language PS = Language.getByLangCode("ps");
    public static final Language GV = Language.getByLangCode("gv");
    public static final Language RUE = Language.getByLangCode("rue");
    public static final Language DV = Language.getByLangCode("dv");
    public static final Language NRM = Language.getByLangCode("nrm");
    public static final Language PAG = Language.getByLangCode("pag");
    public static final Language PA = Language.getByLangCode("pa");
    public static final Language KOI = Language.getByLangCode("koi");
    public static final Language RM = Language.getByLangCode("rm");
    public static final Language KM = Language.getByLangCode("km");
    public static final Language KV = Language.getByLangCode("kv");
    public static final Language UDM = Language.getByLangCode("udm");
    public static final Language CSB = Language.getByLangCode("csb");
    public static final Language MHR = Language.getByLangCode("mhr");
    public static final Language FUR = Language.getByLangCode("fur");
    public static final Language MT = Language.getByLangCode("mt");
    public static final Language ZEA = Language.getByLangCode("zea");
    public static final Language WUU = Language.getByLangCode("wuu");
    public static final Language LIJ = Language.getByLangCode("lij");
    public static final Language UG = Language.getByLangCode("ug");
    public static final Language LAD = Language.getByLangCode("lad");
    public static final Language PI = Language.getByLangCode("pi");
    public static final Language XMF = Language.getByLangCode("xmf");
    public static final Language SC = Language.getByLangCode("sc");
    public static final Language BH = Language.getByLangCode("bh");
    public static final Language ZH_CLASSICAL = Language.getByLangCode("zh-classical");
    public static final Language OR = Language.getByLangCode("or");
    public static final Language NOV = Language.getByLangCode("nov");
    public static final Language KSH = Language.getByLangCode("ksh");
    public static final Language ANG = Language.getByLangCode("ang");
    public static final Language SO = Language.getByLangCode("so");
    public static final Language KW = Language.getByLangCode("kw");
    public static final Language STQ = Language.getByLangCode("stq");
    public static final Language NV = Language.getByLangCode("nv");
    public static final Language HAK = Language.getByLangCode("hak");
    public static final Language FRR = Language.getByLangCode("frr");
    public static final Language AY = Language.getByLangCode("ay");
    public static final Language FRP = Language.getByLangCode("frp");
    public static final Language EXT = Language.getByLangCode("ext");
    public static final Language SZL = Language.getByLangCode("szl");
    public static final Language PCD = Language.getByLangCode("pcd");
    public static final Language IE = Language.getByLangCode("ie");
    public static final Language GAG = Language.getByLangCode("gag");
    public static final Language HAW = Language.getByLangCode("haw");
    public static final Language XAL = Language.getByLangCode("xal");
    public static final Language LN = Language.getByLangCode("ln");
    public static final Language RW = Language.getByLangCode("rw");
    public static final Language PDC = Language.getByLangCode("pdc");
    public static final Language PFL = Language.getByLangCode("pfl");
    public static final Language VEP = Language.getByLangCode("vep");
    public static final Language KRC = Language.getByLangCode("krc");
    public static final Language CRH = Language.getByLangCode("crh");
    public static final Language EML = Language.getByLangCode("eml");
    public static final Language GN = Language.getByLangCode("gn");
    public static final Language ACE = Language.getByLangCode("ace");
    public static final Language TO = Language.getByLangCode("to");
    public static final Language CE = Language.getByLangCode("ce");
    public static final Language KL = Language.getByLangCode("kl");
    public static final Language ARC = Language.getByLangCode("arc");
    public static final Language MYV = Language.getByLangCode("myv");
    public static final Language DSB = Language.getByLangCode("dsb");
    public static final Language AS = Language.getByLangCode("as");
    public static final Language BJN = Language.getByLangCode("bjn");
    public static final Language PAP = Language.getByLangCode("pap");
    public static final Language TPI = Language.getByLangCode("tpi");
    public static final Language LBE = Language.getByLangCode("lbe");
    public static final Language MDF = Language.getByLangCode("mdf");
    public static final Language WO = Language.getByLangCode("wo");
    public static final Language JBO = Language.getByLangCode("jbo");
    public static final Language KAB = Language.getByLangCode("kab");
    public static final Language SN = Language.getByLangCode("sn");
    public static final Language AV = Language.getByLangCode("av");
    public static final Language CBK_ZAM = Language.getByLangCode("cbk-zam");
    public static final Language TY = Language.getByLangCode("ty");
    public static final Language SRN = Language.getByLangCode("srn");
    public static final Language KBD = Language.getByLangCode("kbd");
    public static final Language LO = Language.getByLangCode("lo");
    public static final Language LEZ = Language.getByLangCode("lez");
    public static final Language AB = Language.getByLangCode("ab");
    public static final Language MWL = Language.getByLangCode("mwl");
    public static final Language LTG = Language.getByLangCode("ltg");
    public static final Language NA = Language.getByLangCode("na");
    public static final Language IG = Language.getByLangCode("ig");
    public static final Language KG = Language.getByLangCode("kg");
    public static final Language TET = Language.getByLangCode("tet");
    public static final Language ZA = Language.getByLangCode("za");
    public static final Language KAA = Language.getByLangCode("kaa");
    public static final Language NSO = Language.getByLangCode("nso");
    public static final Language ZU = Language.getByLangCode("zu");
    public static final Language RMY = Language.getByLangCode("rmy");
    public static final Language CU = Language.getByLangCode("cu");
    public static final Language TN = Language.getByLangCode("tn");
    public static final Language CHR = Language.getByLangCode("chr");
    public static final Language CHY = Language.getByLangCode("chy");
    public static final Language GOT = Language.getByLangCode("got");
    public static final Language SM = Language.getByLangCode("sm");
    public static final Language BI = Language.getByLangCode("bi");
    public static final Language MO = Language.getByLangCode("mo");
    public static final Language BM = Language.getByLangCode("bm");
    public static final Language IU = Language.getByLangCode("iu");
    public static final Language PIH = Language.getByLangCode("pih");
    public static final Language IK = Language.getByLangCode("ik");
    public static final Language SS = Language.getByLangCode("ss");
    public static final Language SD = Language.getByLangCode("sd");
    public static final Language PNT = Language.getByLangCode("pnt");
    public static final Language CDO = Language.getByLangCode("cdo");
    public static final Language EE = Language.getByLangCode("ee");
    public static final Language HA = Language.getByLangCode("ha");
    public static final Language TI = Language.getByLangCode("ti");
    public static final Language BXR = Language.getByLangCode("bxr");
    public static final Language TS = Language.getByLangCode("ts");
    public static final Language OM = Language.getByLangCode("om");
    public static final Language KS = Language.getByLangCode("ks");
    public static final Language KI = Language.getByLangCode("ki");
    public static final Language VE = Language.getByLangCode("ve");
    public static final Language SG = Language.getByLangCode("sg");
    public static final Language RN = Language.getByLangCode("rn");
    public static final Language CR = Language.getByLangCode("cr");
    public static final Language DZ = Language.getByLangCode("dz");
    public static final Language LG = Language.getByLangCode("lg");
    public static final Language AK = Language.getByLangCode("ak");
    public static final Language FF = Language.getByLangCode("ff");
    public static final Language TUM = Language.getByLangCode("tum");
    public static final Language FJ = Language.getByLangCode("fj");
    public static final Language ST = Language.getByLangCode("st");
    public static final Language TW = Language.getByLangCode("tw");
    public static final Language XH = Language.getByLangCode("xh");
    public static final Language CH = Language.getByLangCode("ch");
    public static final Language NY = Language.getByLangCode("ny");
    public static final Language NG = Language.getByLangCode("ng");
    public static final Language II = Language.getByLangCode("ii");
    public static final Language CHO = Language.getByLangCode("cho");
    public static final Language MH = Language.getByLangCode("mh");
    public static final Language AA = Language.getByLangCode("aa");
    public static final Language KJ = Language.getByLangCode("kj");
    public static final Language HO = Language.getByLangCode("ho");
    public static final Language MUS = Language.getByLangCode("mus");
    public static final Language KR = Language.getByLangCode("kr");
    public static final Language HZ = Language.getByLangCode("hz");

}
