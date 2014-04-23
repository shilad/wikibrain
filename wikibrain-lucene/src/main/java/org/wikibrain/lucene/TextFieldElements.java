package org.wikibrain.lucene;

/**
 * TextFieldElements is a builder pattern that dictates how a
 * TextFieldBuilder should build a specific Lucene TextField.
 * It also contains a method to retrieve the name of the generated
 * TextField, and a few static TextField names for convenience.
 *
 * TextField elements consist of the page title, redirect synonyms,
 * and the page plain text. To add the title, either use addTitle()
 * to add the title once, or specify an int to add the title multiple
 * times. Redirect synonyms and plain text can only be added once.
 *
 * @author Ari Weiland
 */
public class TextFieldElements {
    private int title;
    private boolean redirects;
    private boolean plainText;

    public TextFieldElements() {
        this.title = 0;
        this.redirects = false;
        this.plainText = false;
    }

    public TextFieldElements addTitle() {
        this.title = 1;
        return this;
    }

    public TextFieldElements addTitle(int i) {
        this.title = i;
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

    public int usesTitle() {
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
        sb.append("title_").append(title).append("_");
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
     * Returns the name of the text field representing only the title, once.
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
        return new TextFieldElements().addRedirects().getTextFieldName();
    }

    /**
     * Returns the name of the text field representing only the plain text.
     *
     * @return
     */
    public static String getPlainTextFieldName() {
        return new TextFieldElements().addPlainText().getTextFieldName();
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
