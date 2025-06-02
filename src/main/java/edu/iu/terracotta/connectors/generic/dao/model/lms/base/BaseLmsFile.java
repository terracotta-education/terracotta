package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;

public interface BaseLmsFile extends LmsEntity<LmsFile>{

    String getId();
    String getDisplayName();
    String getFilename();
    long getSize();
    String getUrl();
    LmsFile from();

}