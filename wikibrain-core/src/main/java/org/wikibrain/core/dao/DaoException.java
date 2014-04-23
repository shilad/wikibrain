package org.wikibrain.core.dao;

public class DaoException extends Exception {
    public DaoException(Exception e) {
        super(e);
    }

    public DaoException(String string) {
        super(string);
    }

    public DaoException(){
        super();
    }

    public DaoException(String string, Exception e){
        super(string, e);
    }
}
