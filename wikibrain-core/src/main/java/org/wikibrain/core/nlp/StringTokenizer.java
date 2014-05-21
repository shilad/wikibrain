package org.wikibrain.core.nlp;

import org.apache.commons.lang3.Range;
import org.wikibrain.core.lang.Language;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Shilad Sen
 */
public class StringTokenizer {

    public List<String> getWords(Language language, String text) {
        List<String> words = new ArrayList<String>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getWordInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                String word = text.substring(lastBoundary, boundary);
                if (word.length() > 0 && Character.isLetterOrDigit(word.charAt(0))) {
                    words.add(word);
                }
            }
            lastBoundary = boundary;
        }
        return words;
    }

    public List<String> getSentences(Language language, String text) {
        List<String> sentences = new ArrayList<String>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                sentences.add(text.substring(lastBoundary, boundary));
            }
            lastBoundary = boundary;
        }
        return sentences;
    }

    public List<Token> getSentenceTokens(Language language, String text) {
        List<Token> sentences = new ArrayList<Token>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                sentences.add(new Token(lastBoundary, boundary, text));
            }
            lastBoundary = boundary;
        }
        return sentences;
    }

    public List<Token> getWordTokens(Language language, String text) {
        List<Token> words = new ArrayList<Token>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getWordInstance(currentLocale);
        sentenceIterator.setText(text);
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                String word = text.substring(lastBoundary, boundary);
                if (word.length() > 0 && Character.isLetterOrDigit(word.charAt(0))) {
                    words.add(new Token(lastBoundary, boundary, text));
                }
            }
            lastBoundary = boundary;
        }
        return words;
    }


    public List<Token> getWordTokens(Language language, Token text) {
        List<Token> words = new ArrayList<Token>();
        Locale currentLocale = language.getLocale();
        BreakIterator sentenceIterator = BreakIterator.getWordInstance(currentLocale);
        sentenceIterator.setText(text.getToken());
        int boundary = sentenceIterator.first();
        int lastBoundary = 0;
        while (boundary != BreakIterator.DONE) {
            boundary = sentenceIterator.next();
            if(boundary != BreakIterator.DONE){
                String word = text.getToken().substring(lastBoundary, boundary);
                if (word.length() > 0 && Character.isLetterOrDigit(word.charAt(0))) {
                    words.add(new Token(
                            lastBoundary + text.getBegin(),
                            boundary + text.getBegin(),
                            text.getFullText()));
                }
            }
            lastBoundary = boundary;
        }
        return words;
    }
}
