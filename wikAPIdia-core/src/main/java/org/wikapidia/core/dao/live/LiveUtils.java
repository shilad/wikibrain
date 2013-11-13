package org.wikapidia.core.dao.live;

import org.apache.commons.io.IOUtils;
import org.wikapidia.core.dao.DaoException;

import java.io.InputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/2/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class LiveUtils {

    public static String getInfoByQuery(String query) throws DaoException {
        String info = new String();
        InputStream inputStr;
        try{
            inputStr = new URL(query).openStream();
            try {
                info = IOUtils.toString(inputStr);
            }
            catch(Exception e){
                throw new DaoException("Error parsing URL");
            }
            finally {
                IOUtils.closeQuietly(inputStr);
            }
        }
        catch(Exception e){
            throw new DaoException("Error getting page from the Wikipedia Server ");
        }

        return info;
    }
}
