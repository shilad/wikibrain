package org.wikibrain.spatial.matcher;

import de.tudarmstadt.ukp.wikipedia.api.hibernate.MetaDataDAO;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.loader.SpatialDataFolder;

import java.io.File;

/**
 * @author Shilad Sen
 */
public class MatchedShapefileLoader {
    private final SpatialDataFolder folder;
    private final SpatialDataDao spatialDao;
    private final MetaInfoDao metaDao;
    private final Env env;

    public MatchedShapefileLoader(Env env) throws ConfigurationException {
        this.env = env;
        this.metaDao = env.getConfigurator().get(MetaInfoDao.class);
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.folder = new SpatialDataFolder(
                new File(env.getConfiguration().get().getString("spatial.dir")));
    }

    public void load(String dataset) {
    }

    public void load(String dataset, String layer) {

    }

    public void main(String args[]) {

    }
}
