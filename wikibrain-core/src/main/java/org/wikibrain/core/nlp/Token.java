package org.wikibrain.core.nlp;

import java.util.List;

/**
 * Captures a token ranging from begin (inclusive) to end (exclusive)
 * @author Shilad Sen
 */
public class Token implements  Comparable<Token> {
    private int begin;
    private int end;
    private String fullText;

    public Token(int begin, int end, String fullText) {
        this.begin = begin;
        this.end = end;
        this.fullText = fullText;
    }

    public Token(List<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        for (Token t : tokens) {
            if (t.begin < i) {
                throw new IllegalArgumentException("overlapping tokens");
            }
            if (t.end < t.begin) {
                throw new IllegalArgumentException("reversed token range");
            }
            i = t.end;
        }
        begin = tokens.get(0).getBegin();
        end = tokens.get(tokens.size() - 1).getEnd();
        this.fullText = tokens.get(0).getFullText();
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public String getFullText() {
        return fullText;
    }

    public String getToken() {
        return fullText.substring(begin, end);
    }

    @Override
    public int compareTo(Token t) {
        int r = begin - t.begin;
        if (r == 0) {
            r = end - t.end;
        }
        return r;
    }
}
