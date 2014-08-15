package org.wikibrain.sr.wikify;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.model.LocalLink;

import java.util.List;

/**
 * @author Shilad Sen
 */
public interface Wikifier {
    List<LocalLink> wikify(int wpId, String text) throws DaoException;

    List<LocalLink> wikify(int wpId) throws DaoException;

    List<LocalLink> wikify(String text) throws DaoException;
}
