package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.MetaInfo;

import java.util.Date;

/**
 * The MetaInfoDao interface stores information about what is loaded into WikAPIdia.
 *
 * @author Shilad Sen
 */
public interface MetaInfoDao extends Dao<MetaInfo> {
    /**
     * Removes all entities from the data store.
     * For a sql table, this will be a "drop."
     *
     * @throws DaoException
     */
    public void clear() throws DaoException;

    /**
     * Clear all entries associated with a component.
     * @param component
     */
    public void clear(Class component) throws DaoException;

    /**
     * Clear all entries associated with a component and language.
     * @param component
     */
    public void clear(Class component, Language lang) throws DaoException;

    /**
     * Increment the count of records for a particular component.
     * Implementations need not write the counts to the database after
     * every increment. Equivalent to calling incrementRecords with lang
     * null.
     *
     * @param component
     * @return The updated record count
     */
    public int incrementRecords(Class component) throws DaoException;

    /**
     * Increment the count of records for a particular component.
     * Implementations need not write the counts to the database after
     * every increment.
     *
     * @param component
     * @param lang
     * @return The updated record count
     */
    public int incrementRecords(Class component, Language lang) throws DaoException;

    /**
     *
     * Increment the count of recrods with errors for a particular component.
     * Implementations need not write the counts to the database after
     * every increment. Equivalent to calling incrementErrors with lang null.
     *
     * @param component
     * @return The updated error count
     */
    public int incrementErrors(Class component) throws DaoException;

    /**
     * Increment the count of recrods with errors for a particular component.
     * Implementations need not write the counts to the database after
     * every increment.
     *
     * @param component
     * @param lang
     * @return The updated error count
     */
    public int incrementErrors(Class component, Language lang) throws DaoException;

    /**
     * Like incrementErrors, but throws no exceptions.
     * @param component
     * @return
     */
    public int incrementErrorsQuietly(Class component);

    /**
     * Like incrementErrors, but throws no exceptions.
     * @param component
     * @param lang
     * @return
     */
    public int incrementErrorsQuietly(Class component, Language lang);

    /**
     * Ensure the counts for all componentsare written to the database.
     */
    public void sync() throws DaoException;

    /**
     * Ensure the counts for a particular component and language are written to the database.
     * @param component
     */
    public void sync(Class component) throws DaoException;

    /**
     * Ensure the counts for a particular component and all languages are written to the database.
     * @param component
     * @param lang
     */
    public void sync(Class component, Language lang) throws DaoException;

    /**
     * Returns the sum of MetaInfo across all languages for the component.
     *
     * Note this captures more than incrementRecords/incrementErrors calls without a language.
     * It captures all calls for the component, with OR without a language.
     *
     * @param component
     * @return
     * @throws DaoException
     */
    MetaInfo getInfo(Class component) throws DaoException;

    /**
     * Returns all languages with at least one record.
     */
    LanguageSet getLoadedLanguages(Class component) throws DaoException;

    /**
     * Returns the current MetaInfo value for the current component.
     *
     * @param component
     * @param lang
     * @return
     * @throws DaoException
     */
    MetaInfo getInfo(Class component, Language lang) throws DaoException;
}
