package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import java.util.Date;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;

public interface BaseLmsAssignment extends LmsEntity<LmsAssignment> {

    int getAllowedAttempts();
    void setAllowedAttempts(int allowedAttempts);
    boolean isCanSubmit();
    void setCanSubmit(boolean canSubmit);
    Class<?> getType();
    String getId();
    String getName();
    void setName(String name);
    boolean isPublished();
    String getSecureParams();
    void setSecureParams(String secureParams);
    Date getDueAt();
    List<String> getSubmissionTypes();
    Float getPointsPossible();
    Date getLockAt();
    Date getUnlockAt();
    LmsAssignment convert();

}
