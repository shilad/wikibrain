package org.wikibrain.cookbook;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.model.LocalPage;

import java.util.Set;

/**
 * @author Shilad Sen
 */
public class Sandbox {
    public static void main(String args[]) throws ConfigurationException {
        Env env = EnvBuilder.envFromArgs(args);
    }
}
