package org.wikibrain.parser.wiki;

import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.RawPage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;


public class SubarticleParser {
    private final LanguageInfo lang;

    public SubarticleParser(LanguageInfo lang) {
        this.lang = lang;
    }

    public ParsedLink.SubarticleType isSeeAlsoHeader(LanguageInfo lang, String headerName){
		if (lang.getSeeAlsoHeaderPattern() != null && headerName != null){
			Matcher m = lang.getSeeAlsoHeaderPattern().matcher(headerName);
			if (m.find()) return ParsedLink.SubarticleType.SEEALSO_HEADER;
		}
		return null;
	}
	
	private static final Pattern templatePipePattern = Pattern.compile("([^\\|]+)"); 
	private static final Pattern templateLinkPattern = Pattern.compile("\\[\\[(.+?)\\]\\]");

	public List<String> getContentsOfTemplatePipe(String templateText){
		
		// to catch links in templates like this in Hebrew: {{ערך מורחב|ערכים=[[ספרות עברית]], [[ספרות ישראלית]]}}
		List<String> rVal = new ArrayList<String>();
		
		Matcher m = templateLinkPattern.matcher(templateText);
		while(m.find()){
			rVal.add(m.group(1));
		}
		
		if (rVal.size() == 0){
			m = templatePipePattern.matcher(templateText);
			int index = 0;
			while(m.find()){
				if (index > 0){ // don't want to include the template name
					rVal.add(m.group(1));
				}
				index++;
			}
		}

		return rVal;
	}
	
	public static String removeTemplateAnchor(String templateText){
		String[] parts = templateText.split("\\{\\{!\\}\\}");
		return parts[0];
	}
	
	public ParsedLink.SubarticleType isTemplateSubarticle(String templateName, String templateText) {
        ParsedLink.SubarticleType rVal = null;
		if (lang.getMainTemplatePattern() != null){
			Matcher m = lang.getMainTemplatePattern().matcher(templateName);
			if (m.find())
				rVal = ParsedLink.SubarticleType.MAIN_TEMPLATE;
		}
		if (rVal == null && lang.getSeeAlsoTemplatePattern() != null){
			Matcher m = lang.getSeeAlsoTemplatePattern().matcher(templateName);
			if (m.find())
				rVal = ParsedLink.SubarticleType.SEEALSO_TEMPLATE;
		}
		if (rVal != null){
			rVal = handleSpecialTemplateBasedSubarticleSpecialCases(templateName, templateText, rVal);
		}
		
		return rVal;
	}
	
	private static int LEFT_WINDOW = 150; // max distance to look for newline char; given that subarticle lines tend to be short, this should be more than enough
	public ParsedLink.SubarticleType isInlineSubarticle(int location, RawPage pageXml){
		if ((lang.getMainInlinePattern() != null || lang.getSeeAlsoInlinePattern() != null) && location > 0){
			
			Boolean valid = false;
			Integer beginningOfValidText = null;
			String window = null;
			Integer searchLoc = null;
			if (location-LEFT_WINDOW > 0){
				int leftBoundary = location-LEFT_WINDOW; 
				window = pageXml.getBody().substring(leftBoundary, location);
				searchLoc = window.length()-1;
				char curChar;
				boolean found = false;
				do{
					curChar = window.charAt(searchLoc);
					if (curChar == '\n' || curChar == '\r'){
						found = true;
					}
					searchLoc--;
				}while(searchLoc >= 0 && !found);
				if (found){
					valid = true;
					beginningOfValidText = leftBoundary+searchLoc;
					checkArgument(beginningOfValidText >= 0, window);
				}
			}else{
				beginningOfValidText = 0;
				valid = true;
			}
			
			if(!valid) return null;

			String textUntilNewLine = pageXml.getBody().substring(beginningOfValidText, location);

			if (lang.getMainInlinePattern() != null){
				Matcher m = lang.getMainInlinePattern().matcher(textUntilNewLine);
				if (m.find()){
					return ParsedLink.SubarticleType.MAIN_INLINE;
				}
			}
			
			if (lang.getSeeAlsoInlinePattern() != null){
				Matcher m = lang.getSeeAlsoInlinePattern().matcher(textUntilNewLine);
				if (m.find()) return ParsedLink.SubarticleType.SEEALSO_INLINE;
			}
		}
		
		return null;
	}

    private final static Pattern special_DanishSeOgs = Pattern.compile("Tekst\\s*=\\s*Se også", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    public ParsedLink.SubarticleType handleSpecialTemplateBasedSubarticleSpecialCases(String templateName,
                                                                         String templateText, ParsedLink.SubarticleType normalType) {
        ParsedLink.SubarticleType rVal = normalType;

        // danish
        if (lang.getLanguage().getLangCode().equals("de")){
            if (normalType.equals(ParsedLink.SubarticleType.MAIN_TEMPLATE)){
                Matcher m = special_DanishSeOgs.matcher(templateText);
                if (m.find()){
                    rVal = ParsedLink.SubarticleType.SEEALSO_TEMPLATE;
                }
            }
        }
        return rVal;
    }

}
