package org.wikibrain.cookbook.regionlabeling;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;

/**
 * Created by Anja Beth Swoap on 6/9/16.
 */
public class RegionDataPoint {
    private int clusterId;
    private String articleTitle;
    private Language lg;

    public RegionDataPoint(int c, String t){
        clusterId = c;
        articleTitle = t;
        lg = Language.SIMPLE;
    }

    public RegionDataPoint(int c, String t, String lang){
        clusterId = c;
        articleTitle = t;
        lg = Language.getByFullLangName(lang);
    }

    public int getClusterId(){
        return clusterId;
    }

    public String getArticleTitle(){
        return articleTitle;
    }

    public LocalPage getCorrespondingPage(Language l) throws Exception{
        Env env = new EnvBuilder().setBaseDir(".").build();
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);

        return lpDao.getByTitle(lg, NameSpace.ARTICLE, articleTitle);
    }



    public String toString(){
        return "Cluster: " + clusterId + " Title : " + articleTitle;
    }
}
