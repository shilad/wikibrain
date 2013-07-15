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
        return this;
    }

    public TextFieldElements addRedirects() {
        this.redirects = true;
        return this;
    }

    public TextFieldElements addPlainText() {
        this.plainText = true;
        return this;
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

    /**
     * Returns a generated name of the text field specified by this instance.
     *
     * @return
     */
    public String getTextFieldName() {
        StringBuilder sb = new StringBuilder();
        if (title) {
            sb.append("title_");
        }
        if (redirects) {
            sb.append("redirects_");
        }
        if (plainText) {
            sb.append("plaintext_");
        }
        sb.append("field");
        return sb.toString();
    }

    /**
     * Returns the name of the text field representing only the title.
     *
     * @return
     */
    public static String getTitleFieldName() {
        return new TextFieldElements().addTitle().getTextFieldName();
    }

    /**
     * Returns the name of the text field representing only redirected titles.
     *
     * @return
     */
    public static String getRedirectsFieldName() {
        return new TextFieldElements().addTitle().getTextFieldName();
    }

    /**
     * Returns the name of the text field representing only the plain text.
     *
     * @return
     */
    public static String getPlainTextFieldName() {
        return new TextFieldElements().addTitle().getTextFieldName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextFieldElements)) return false;
        TextFieldElements opts = (TextFieldElements) o;
        return (this.title == opts.title &&
                this.redirects == opts.redirects &&
                this.plainText == opts.plainText);
    }
}