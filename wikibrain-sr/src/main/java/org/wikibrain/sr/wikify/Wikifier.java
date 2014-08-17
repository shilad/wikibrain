package org.wikibrain.sr.wikify;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.model.LocalLink;

import java.util.List;

/**
 * @author Shilad Sen
 */
public interface Wikifier {
    public List<LocalLink> wikify(int wpId, String text) throws DaoException;

    public List<LocalLink> wikify(int wpId) throws DaoException;

    public List<LocalLink> wikify(String text) throws DaoException;
}
