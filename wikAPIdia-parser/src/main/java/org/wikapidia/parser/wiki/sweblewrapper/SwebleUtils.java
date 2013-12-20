package org.wikapidia.parser.wiki.sweblewrapper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.sweble.wikitext.engine.config.Interwiki;
import org.sweble.wikitext.engine.config.WikiConfig;

public class SwebleUtils {
	public static HashSet<String> extractInterWikiPrefixSet(WikiConfig config){
		HashSet<String> prefixSet = new HashSet<String>();
		for(Interwiki interWiki : config.getInterwikis()){
			prefixSet.add(interWiki.getPrefix());
		}
		return prefixSet;
	}
	
	public static String cleanWikiText(String wikiText) {
		return wikiText.replaceAll("(?i)[<](/)?img[^>]*[>]", " ")
				.replaceAll("(?i)[<]caption[^/>]*[>]", "|+")
				.replaceAll("(?i)</caption>", " ")
				.replaceAll("(?i)[<]table[^/>]*class=\"wikitable\"[^/>]*[>]", "{| class=\"wikitable\"")
				.replaceAll("(?i)[<]table[^/>]*[>]", "{|")
				.replaceAll("(?i)[<]tr[^/>]*[>]", "\n|-")
				.replaceAll("(?i)[<]td[^/>]*[>]", "\n|")
				.replaceAll("(?i)[<]th[^/>]*[>]", "\n!")
				.replaceAll("(?i)</table>", "\n|}")
				.replaceAll("(?i)</tr>", "\n")
				.replaceAll("(?i)</td>", "\n")
				.replaceAll("(?i)</th>", "\n")
				.replaceAll("(?i)(<gallery>)[^(</gallery>)]*(</gallery>)", " ")
				.replaceAll("(?i)(<ref>)[^(</ref>)]*(</ref>)", " ")
				.replaceAll("(?i)[<](/)?[\\w]+[^>\n<]*[>]", " ")
						;
	}
	
	
	
	public static String cleanHTML(String html) {
		return html.replaceAll("(?i)[<]table[^/>]*[>][^\\w^[</table>]]*[</table>]", "");
	}
	
	
	public static String removeInterWikiPrefix(String text, HashSet<String> interWikiPrefixSet){
		int prefixIdx = text.indexOf(':');
		if(prefixIdx == -1) return text;
		String prefix = text.substring(0, prefixIdx);
		if(interWikiPrefixSet.contains(prefix)){
			return text.substring(prefixIdx+1, text.length());
		}
		return text;
	}
	
	public static String replaceTags(String text, Character replacingChar){
		Pattern p = Pattern.compile("</?\\w+[^>\n]*>");
		return replacePattern(p, text, replacingChar);
	}
	
	public static String replaceHTMLEntities(String text, Character replacingChar){
		Pattern p = Pattern.compile("&(([a-zA-Z^;]+)||(#[0-9^;]+));");
		Matcher m = p.matcher(text);
		StringBuilder sb = new StringBuilder();
		int curIdx = 0;
		while (m.find()) {
			sb.append(text.substring(curIdx, m.start()));
			String entity = text.substring(m.start(),m.end());
			try{
				String decodedEntity = StringEscapeUtils.unescapeHtml4(entity);
				sb.append(decodedEntity);
				sb.append(repeatChars(replacingChar, m.end()-m.start()-decodedEntity.length()));
				curIdx = m.end();
			} catch(IllegalArgumentException e){
				
			}
		}
		sb.append(text.substring(curIdx));
		return sb.toString();
	}
	
	private static String replacePattern(Pattern p, String text, Character replacingChar){
		Matcher m = p.matcher(text);
		StringBuilder sb = new StringBuilder();
		int curIdx = 0;
		while (m.find()) {
			sb.append(text.substring(curIdx, m.start()));
			sb.append(repeatChars(replacingChar, m.end()-m.start()));
			curIdx = m.end();
		}
		sb.append(text.substring(curIdx));
		return sb.toString();
	}
	
	private static String repeatChars(Character ch, int length){
		if(ch == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<length; i++){
			sb.append(ch);
		}
		return sb.toString();
	}
	
	public static void writeText(String filename, String text){
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream(filename));
			out.print(text);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
