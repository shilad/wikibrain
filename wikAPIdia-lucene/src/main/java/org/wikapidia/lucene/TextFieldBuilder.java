package org.wikapidia.lucene;

import gnu.trove.iterator.TIntIterator;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.WikiTextParser;

/**
 * This class builds custom Lucene TextFields from pages based on
 * title, title synonyms as defined by redirects, and plain text.
 *
 * @author Ari Weiland
 */
public class TextFieldBuilder {

    private final LocalPageDao localPageDao;
    private final RawPageDao rawPageDao;
    private final RedirectDao redirectDao;
    private final WikiTextParser parser;

    public TextFieldBuilder(WikiTextParser parser, LocalPageDao localPageDao, RawPageDao rawPageDao, RedirectDao redirectDao) {
        this.localPageDao = localPageDao;
        this.rawPageDao = rawPageDao;
        this.redirectDao = redirectDao;
        this.parser = parser;
        try {
            localPageDao.setFollowRedirects(false);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a lucene text field for page based on the specified text field elements
     *
     * @param page
     * @param elements
     * @return
     * @throws DaoException
     */
    public TextField buildTextField(LocalPage page, TextFieldElements elements) throws DaoException {
        return buildTextField(
                page,
                rawPageDao.getById(page.getLanguage(), page.getLocalId()),
                elements);
    }

    /**
     * Builds a lucene text field for page based on the specified text field elements
     *
     * @param page
     * @param elements
     * @return
     * @throws DaoException
     */
    public TextField buildTextField(RawPage page, TextFieldElements elements) throws DaoException {
        return buildTextField(
                localPageDao.getById(page.getLanguage(), page.getLocalId()),
                page,
                elements);
    }

    private TextField buildTextField(LocalPage localPage, RawPage rawPage, TextFieldElements elements) throws DaoException {
        StringBuilder sb = new StringBuilder();
        String title = rawPage.getTitle().getCanonicalTitle();
        for (int i=0; i<elements.usesTitle(); i++) {
            sb.append(title);
            sb.append(" ");
        }
        if (elements.usesRedirects()) {
            TIntIterator iterator = redirectDao.getRedirects(localPage).iterator();
            while (iterator.hasNext()) {
                sb.append(localPageDao
                        .getById(localPage.getLanguage(), iterator.next())
                        .getTitle()
                        .getCanonicalTitle());
                sb.append(" ");
            }
        }
        if (elements.usesPlainText()) {
            try {
                sb.append(parser.getPlainText(rawPage));
            } catch (WikapidiaException e) {
                throw new DaoException(e);
            }
        }
        return new TextField(elements.getTextFieldName(), sb.toString().trim(), Field.Store.YES);
    }

}
