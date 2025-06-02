package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFolder;

public interface BaseLmsFolder extends LmsEntity<LmsFolder> {

    String getId();
    String getName();
    String getFullName();
    String getFilesUrl();

}
