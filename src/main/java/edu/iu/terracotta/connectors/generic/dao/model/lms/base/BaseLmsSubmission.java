package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;

public interface BaseLmsSubmission extends LmsEntity<LmsSubmission> {

    Class<?> getType();
    Double getScore();
    Object getUser();
    String getUserId();
    String getUserLoginId();
    String getUserName();
    Long getAttempt();
    String getAssignmentId();
    LmsSubmission from();

}
