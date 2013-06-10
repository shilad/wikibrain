package org.wikapidia.core.provider;

import com.jolbox.bonecp.BoneCPDataSource;
import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;

import javax.sql.DataSource;


/**
 * Configures a datasource based on a configuration file.
 * An example configuration:
 *
 *  foo : {
 *      driver : org.h2.Driver
 *      url: jdbc:h2:path/to/db
 *      username : sa
 *      password : ""
 *  }
 *
 *  BoneCP is used for connection pooling.
 */
public class DataSourceProvider extends Provider<DataSource> {

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     *
     * @param configurator
     * @param config
     */
    public DataSourceProvider(Configurator configurator, Configuration config) throws ConfigurationException {
        super(configurator, config);
    }

    @Override
    public Class getType() {
        return DataSource.class;
    }

    @Override
    public DataSource get(String name, Config config) throws ConfigurationException {
        try {
            Class.forName(config.getString("driver"));
            BoneCPDataSource ds = new BoneCPDataSource();
            ds.setJdbcUrl(config.getString("url"));
            ds.setUsername(config.getString("username"));
            ds.setPassword(config.getString("password"));
            return ds;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(e);
        }
    }
}
