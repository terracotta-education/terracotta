package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;

public interface BaseLmsUser extends LmsEntity<LmsUser> {

    String getEmail();
    String getId();
    LmsUser from();

}
