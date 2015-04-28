package org.wikibrain.core.lang;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.WikiBrainException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * Provides access to language-specific parsing information.
 * The data is loaded from tsv resources.
 */
public class LanguageInfo {
    private static Logger LOG = LoggerFactory.getLogger(LanguageInfo.class);

    private static final String INFO_FILENAME = "language_info.tsv";

    /**
     * All Language parser information.
     * This array is parallel with the Language array.
     * Since we don't have parsing information for some languages, some values will be null.
     */
    public static LanguageInfo LANGUAGE_INFOS[] =
            new LanguageInfo[Language.LANGUAGES.length];

    static {
        InputStream stream = null;
        try {
            stream = Language.class.getClassLoader()
                    .getResourceAsStream(INFO_FILENAME);
            loadAllLanguages(stream);
        } catch (WikiBrainException e) {
            throw new RuntimeException(e);  // What else can we do?
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }

    public static LanguageInfo getByLanguage(Language lang) {
        return LANGUAGE_INFOS[lang.getId() - 1];
    }
    public static LanguageInfo getById(int langId) {
        return LANGUAGE_INFOS[langId-1];
    }
    public static LanguageInfo getByLangCode(String langCode) {
        return LANGUAGE_INFOS[Language.getByLangCode(langCode).getId() - 1];
    }

    private Language language;

    private List<String> categoryNames = new ArrayList<String>();
    private List<String> disambiguationCategoryNames = new ArrayList<String>();
    private List<AltNamespaceStruct> alternativeArticleNamespaces = new ArrayList<AltNamespaceStruct>();

    private Pattern redirectPattern = null;
    private Pattern categoryPattern = null;
    private Pattern defaultCategoryPattern = null;
    private Pattern categoryReplacePattern = null;
    private Pattern mainTemplatePattern = null;
    private Pattern seeAlsoTemplatePattern = null;
    private Pattern mainInlinePattern = null;
    private Pattern seeAlsoInlinePattern = null;
    private Pattern seeAlsoHeaderPattern = null;

    private int numLinks;
    private int numArticles;

    private LanguageInfo(Language language) {
        this.language = language;
    }

    public boolean hasAlternativeArticleNamespaces(){
        return (alternativeArticleNamespaces.size() > 0);
    }

    public void setRedirectNames(List<String> names) {
        String rd_pattern = list2NonCapturingGroup(names)+":{0,1}\\s*\\[\\[(.+?)\\]\\]";
        redirectPattern = Pattern.compile(rd_pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public void setCategoryNames(List<String> names) {
        this.categoryNames = names;

        String cat_pattern = "\\A"+list2NonCapturingGroup(names)+ "\\s*:\\s*(.+)";
        categoryPattern = Pattern.compile(cat_pattern,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        String default_cat_pattern = "\\A"+names.get(0)+ "\\s*:\\s*(.+)";
        defaultCategoryPattern = Pattern.compile(default_cat_pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        String cat_replace_pattern = "\\A("+list2NonCapturingGroup(names)+ ")\\s*:\\s*(.+)";
        categoryReplacePattern = Pattern.compile(cat_replace_pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public void setMainTemplates(List<String> names) {
        if (names.size() > 0){
            String maintemplate_pattern = "\\A"+list2NonCapturingGroup(names)+"\\Z";
            mainTemplatePattern = Pattern.compile(maintemplate_pattern,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void setSeeAlsoTemplates(List<String> names) {
        if (names.size() > 0){
            String seealso_pattern = "\\A"+list2NonCapturingGroup(names)+"\\Z";
            seeAlsoTemplatePattern = Pattern.compile(seealso_pattern,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void setSeeAlsoHeaders(List<String> names) {
        if (names.size() > 0){
            String seealsoheader_pattern = "\\A"+list2NonCapturingGroup(names)+"\\Z";
            seeAlsoHeaderPattern = Pattern.compile(seealsoheader_pattern,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void setMainInlines(List<String> names) {
        if (names.size() > 0){
            String maininline_pattern = list2NonCapturingGroup(names);
            mainInlinePattern = Pattern.compile(maininline_pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void setSeeAlsoInlines(List<String> names) {
        if (names.size() > 0){
            String seealsoinline_pattern = list2NonCapturingGroup(names);
            seeAlsoInlinePattern = Pattern.compile(seealsoinline_pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public void setDisambiguationCategoryNames(List<String> names) {
        this.disambiguationCategoryNames = names;
    }

    public void setAlternativeArticleNamespaces(String cell) {
        this.alternativeArticleNamespaces = getAltNamespaces(cell);
    }

    public Language getLanguage() {
        return language;
    }

    public List<String> getCategoryNames() {
        return this.categoryNames;
    }

    public List<String> getDisambiguationCategoryNames() {
        return disambiguationCategoryNames;
    }

    public List<AltNamespaceStruct> getAlternativeArticleNamespaces() {
        return alternativeArticleNamespaces;
    }

    public Pattern getRedirectPattern() {
        return redirectPattern;
    }

    public Pattern getCategoryPattern() {
        return categoryPattern;
    }

    public Pattern getDefaultCategoryPattern() {
        return defaultCategoryPattern;
    }

    public Pattern getCategoryReplacePattern() {
        return categoryReplacePattern;
    }

    public Pattern getMainTemplatePattern() {
        return mainTemplatePattern;
    }

    public Pattern getSeeAlsoTemplatePattern() {
        return seeAlsoTemplatePattern;
    }

    public Pattern getMainInlinePattern() {
        return mainInlinePattern;
    }

    public Pattern getSeeAlsoInlinePattern() {
        return seeAlsoInlinePattern;
    }

    public Pattern getSeeAlsoHeaderPattern() {
        return seeAlsoHeaderPattern;
    }

    public static class AltNamespaceStruct{
        public final String prefix;
        public final Integer nsId;

        public AltNamespaceStruct(String scsv){
            String[] parts = scsv.split(";");
            this.prefix = parts[0];
            this.nsId = Integer.parseInt(parts[1]);
        }

        @Override
        public String toString(){
            return prefix + ";" + nsId;
        }

        @Override
        public boolean equals(Object o){
            if (o instanceof String){
                return (this.prefix.equals(o));
            }else if(o instanceof AltNamespaceStruct){
                AltNamespaceStruct ans = (AltNamespaceStruct)o;
                return  (ans.nsId.equals(nsId) && ans.prefix.equals(o));
            }else{
                return false;
            }
        }
    }

    private List<AltNamespaceStruct> getAltNamespaces(String cell){
        List<AltNamespaceStruct> rVal = new ArrayList<AltNamespaceStruct>();
        if (cell.length() > 0){
            String[] nss = cell.split(",");
            for(String ns : nss){
                AltNamespaceStruct ans = new AltNamespaceStruct(ns);
                rVal.add(ans);
            }
        }
        return rVal;
    }

    public int getNumLinks() {
        return numLinks;
    }

    public int getNumArticles() {
        return numArticles;
    }

    public void setNumLinks(int numLinks) {
        this.numLinks = numLinks;
    }

    public void setNumArticles(int numArticles) {
        this.numArticles = numArticles;
    }

    public String getDefaultCategoryNamespaceName(){
        return categoryNames.get(0);
    }

    private static List<String> csv2List(String csv){
        if (csv.length() > 0){
            if (csv.charAt(0)=='"'&&csv.charAt(csv.length()-1)=='"'){
                csv = csv.substring(1,csv.length()-1);
            }
            return Arrays.asList(csv.split(","));
        }else{
            return new ArrayList<String>();
        }
    }

    public static String list2NonCapturingGroup(Collection<String> list){
        return "(?:" + StringUtils.join(list, "|") + ")";
    }

    private static void loadLanguage(String fields[], String line) throws WikiBrainException {
        String tokens[] = StringUtils.splitPreserveAllTokens(line, "\t");
        if (tokens.length != fields.length) {
            throw new WikiBrainException("invalid number of fields in " + StringEscapeUtils.escapeJava(line));
        }
        Language lang = Language.getByLangCode(tokens[0]);
        LanguageInfo info = new LanguageInfo(lang);
        for (int i = 1; i < fields.length; i++) {
            if (fields[i].equals("alternativeArticleNamespaces")) {
                info.setAlternativeArticleNamespaces(tokens[i]);
            } else {
                try {
                    if (fields[i].startsWith("num")) {
                        BeanUtils.setProperty(info, fields[i], Integer.valueOf(tokens[i]));
                    } else {
                        BeanUtils.setProperty(info, fields[i], csv2List(tokens[i]));
                    }
                } catch (IllegalAccessException e) {
                    throw new WikiBrainException("unknown property in LanguageInfo: " + fields[i]);
                } catch (InvocationTargetException e) {
                    throw new WikiBrainException(e);
                }
            }
        }
        LANGUAGE_INFOS[lang.getId() - 1] = info;
    }

    private static void loadAllLanguages(InputStream stream) throws WikiBrainException {
        List<String> lines;
        try {
            lines = IOUtils.readLines(stream, "UTF-8");
        } catch (IOException e) {
            throw new WikiBrainException(e);
        }

        // read and validate header
        String fields[] = StringUtils.splitPreserveAllTokens(lines.get(0), "\t");
        if (!fields[0].equals("langCode")) {
            throw new WikiBrainException(
                    "invalid header in " + INFO_FILENAME +
                    ": " + StringEscapeUtils.escapeJava(lines.get(0)));
        }

        for (String line : lines.subList(1, lines.size())) {
            loadLanguage(fields, line);
        }
    }
}
