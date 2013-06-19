package org.wikapidia.dao.load;

import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;

/**
 */
public class LinkResolver {
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;

     public LinkResolver(LocalLinkDao linkDao, LocalPageDao pageDao, LocalCategoryMemberDao catMemDao){
         this.linkDao = linkDao;
         this.pageDao = pageDao;
         this.catMemDao = catMemDao;
     }


}
