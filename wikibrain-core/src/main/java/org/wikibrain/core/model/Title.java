package org.wikibrain.core.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.utils.WpStringUtils;

import java.io.*;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a canonically capitalized Wikipedia title.
 * Contains utility methods for querying aspects of the title.
 *
 */
public class Title implements Externalizable {

    private String canonicalTitle;
    private LanguageInfo language;

	private static final long serialVersionUID = 3L;
	private static final Logger LOG = LoggerFactory.getLogger(Title.class);

    public Title(String text, LanguageInfo language) {
        this(text, false, language);
    }

    public Title(String text, boolean isCanonical, LanguageInfo lang) {
        this.canonicalTitle = isCanonical ? text : canonicalize(text, lang);
        this.language = lang;
    }

    public Title(String title, Language language) {
        this(title, LanguageInfo.getByLanguage(language));
    }

    public String getCanonicalTitle() {
        return canonicalTitle;
    }

    public LanguageInfo getLanguageInfo() {
        return language;
    }
    public Language getLanguage() {
        return language.getLanguage();
    }

    public NameSpace getNamespace(){
        Matcher m = language.getCategoryPattern().matcher(canonicalTitle);
        if (m.find()) {
            return NameSpace.CATEGORY;
        }
        String nameSpaceString = this.getNamespaceString();
        if (nameSpaceString==null){
            return NameSpace.ARTICLE;
        } else {
            return NameSpace.getNameSpaceByName(nameSpaceString);
        }
    }

    /**
	 * Gets the "Category:" or equivalent
	 * @return
	 */
	public String getNamespaceString(){
		return getNamespaceString(this.canonicalTitle);
	}
	
	private static String getNamespaceString(String text){
        if (text.equals(":")) {
            return null;
        }
		String[] parts = text.split(":");
		if (parts != null && parts.length > 0 && text.contains(":")&& NameSpace.isNamespaceString(parts[0])){
			return parts[0];
		}else{
			return null;
		}
	}
	
	/**
	 * Gets the part of the title after the first colon. If there is no
	 * colon, returns the whole title.
	 * @return
	 */
	public String getTitleStringWithoutNamespace(){
		return getTitleStringWithoutNamespace(canonicalTitle);
	}

    private static String getTitleStringWithoutNamespace(String text){
        String[] parts = text.split(":",2);

        if (parts.length == 1 || !NameSpace.isNamespaceString(parts[0])) {
            return text;
        } else {
            return parts[1].trim();
        }
    }
	
	@Override
	public String toString(){
		return canonicalTitle + " (" + getLanguage().getLangCode() + ")";
	}
	
	@Override
	public int hashCode(){
		return canonicalTitle.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof Title){
			return o.toString().equals(this.toString());
		}else{
			return false;
		}
	}
	
	/**
	 * Needs langId because Title does not store it for memory reasons
	 * @return
	 */
	public Title toUpperCase(){
		String upTitle = this.toString().toUpperCase();
		return new Title(upTitle, true, language);
	}
	
	public String toUrl() throws WikiBrainException{
		try {
			return "http://" + language.getLanguage().getDomain() + "/wiki/" + URLEncoder.encode(getCanonicalTitle().replaceAll(" ", "_"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new WikiBrainException(e);
		}
	}

    public long longHashCode() {
        return longHashCode(language.getLanguage(), getCanonicalTitle(), getNamespace());
    }

    public static long longHashCode(Language l, String title, NameSpace ns) {
        return longHashCode(l.getId(), title, ns.getArbitraryId());
    }

    public static long longHashCode(int langId, String title, int nsArbitraryId) {
        return WpStringUtils.longHashCode(langId + "." + nsArbitraryId + "." + title);
    }

    /**
     * Deconstructs a title such as "Mash_(film)" into {"Mash", "film"}
     */
	private static Pattern nameAndDisambiguatorPattern = Pattern.compile("(.+?)\\s*\\((.+?)\\)");
	public String[] getNameAndDisambiguator(){
		String s = this.toString();
		Matcher m = nameAndDisambiguatorPattern.matcher(s);
		String disam = null;
		String name = this.toString();
		if (m.find()){
			name = m.group(1);
			disam = m.group(2);
		}
		return new String[] {name, disam};

	}

    private static final Pattern COLON_PATTERN = Pattern.compile("\\s+:\\s+");

    /**
     * Converts a title into its canonical Wikipedia representation.
     * This may be imperfect, but it's a good guess.
     * @param title
     * @param lang
     * @return
     */
    public static String canonicalize(String title, LanguageInfo lang) {

        // spaces and underscores are equivalent
        title = title.replaceAll("_", " ");

        // remove the fragment
        int i = title.indexOf("#");
        if (i >= 0) {
            title = title.substring(0, i);
        }

        //removes leading and trailing spaces
        title = title.trim();


        // This code enforces the "first letter always caps, everything else
        // is case-sensitive policy" of Wikipedia. I modified this code from JWPL 0.9.1
        // just to be safe, but I think it's identical to my old code.
        title = StringUtils.capitalize(title);

        // handle whitespace around colons (only a problem for categories in this context)
        Matcher m = COLON_PATTERN.matcher(title);
        if (m.find()){
            title = m.replaceFirst(":");
        }

        // normalize all the category aliases
        m = lang.getCategoryReplacePattern().matcher(title);
        if (m.find() && m.group(1).equals(lang.getDefaultCategoryNamespaceName())) {
            title =  lang.getDefaultCategoryNamespaceName() + ":" + m.group(2);
        }

        // ensure reasonable capitalization with namespaces
        if (getNamespaceString(title) != null){
            // make sure that titles following colons conform to first-letter-caps policy
            title = getNamespaceString(title) + ":" + StringUtils.capitalize(getTitleStringWithoutNamespace(title));
        }

        // this is a weird f-ing bug that needed to be fixed!
        title = title.replaceAll("\u200E", "");

        return title;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.canonicalTitle = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(canonicalTitle);
    }
}
