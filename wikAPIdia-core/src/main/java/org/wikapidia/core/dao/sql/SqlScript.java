package org.wikapidia.core.dao.sql;

/**
* @author Shilad Sen
*/
public class SqlScript {
    Type type;
    String path;

    // Path is presumed to be a resource on the classpath
    // i.e. db/localpage-create-tables.sql
    public SqlScript(Type type, String path) {
        this.type = type;
        this.path = path;
    }
    public Type getType() { return type; }
    public String getPath() { return path; }

    enum Type {
        CREATE_TABLES,
        DROP_TABLES,
        CREATE_INDEXES,
        DROP_INDEXES,
    }
}
