package org.wikibrain.sr.wikify;

/**
 * @author Shilad Sen
 */
public class IdAndText {
    private int id;
    private String text;


    public IdAndText(String text) {
        this(-1, text);
    }

    public IdAndText(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
