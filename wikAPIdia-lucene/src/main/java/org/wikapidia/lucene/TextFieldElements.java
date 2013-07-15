package org.wikapidia.lucene;

public class TextFieldElements {
    private boolean title;
    private boolean redirects;
    private boolean plainText;

    public TextFieldElements() {
        this.title = false;
        this.redirects = false;
        this.plainText = false;
    }

    public TextFieldElements addTitle() {
        this.title = true;
        return null;
    }

    public TextFieldElements addRedirects() {
        this.redirects = true;
        return null;
    }

    public TextFieldElements addPlainText() {
        this.plainText = true;
        return null;
    }

    public boolean usesTitle() {
        return title;
    }

    public boolean usesRedirects() {
        return redirects;
    }

    public boolean usesPlainText() {
        return plainText;
    }
}