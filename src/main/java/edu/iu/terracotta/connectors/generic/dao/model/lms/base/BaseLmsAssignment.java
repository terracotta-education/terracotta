package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import java.util.Date;
import java.util.List;
import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

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
    LmsAssignment from();
    LmsExternalToolFields getLmsExternalToolFields();
    void setLmsExternalToolFields(LmsExternalToolFields lmsExternalToolFields);
    String getGradingType();
    String addMetadata(String key, Map<String, Object> values) throws TerracottaConnectorException;

}
