package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;

public interface BaseLmsSubmission extends LmsEntity<LmsSubmission> {

    Class<?> getType();
    Double getScore();
    Object getUser();
    Long getUserId();
    String getUserLoginId();
    String getUserName();
    Long getAttempt();
    LmsSubmission from();

}
