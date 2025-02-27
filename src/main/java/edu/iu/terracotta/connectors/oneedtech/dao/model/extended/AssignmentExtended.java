package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Assignment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class AssignmentExtended extends LmsAssignment {

    @Builder.Default private Assignment assignment = Assignment.builder().build();

    @Override
    public String getId() {
        return assignment.getId();
    }

    @Override
    public String getSecureParams() {
        return Base64.getEncoder()
            .encodeToString(
                String.format(
                    "{\"lti_assignment_id\":\"%s\"}",
                    assignment.getResourceLinkId()
                )
                .getBytes()
            );
    }

    @Override
    public void setSecureParams(String secureParams) {
        super.setSecureParams(secureParams);
    }

    @Override
    public int getAllowedAttempts() {
        return 0;
    }

    @Override
    public void setAllowedAttempts(int allowedAttempts) {
        super.setAllowedAttempts(allowedAttempts);
    }

    @Override
    public boolean isCanSubmit() {
        return true;
    }

    @Override
    public void setCanSubmit(boolean canSubmit) {
        super.setCanSubmit(canSubmit);
    }

    @Override
    public String getName() {
        return assignment.getLabel();
    }

    @Override
    public void setName(String name) {
        assignment.setLabel(name);
    }

    @Override
    public boolean isPublished() {
        return true;
    }

    @Override
    public Date getDueAt() {
        return null;
    }

    @Override
    public List<String> getSubmissionTypes() {
        return Collections.singletonList("external_tool");
    }

    @Override
    public Float getPointsPossible() {
        return assignment.getScoreMaximum();
    }

    @Override
    public Date getLockAt() {
        return null;
    }

    @Override
    public Date getUnlockAt() {
        return null;
    }

    @Override
    public LmsAssignment from() {
        LmsAssignment convertedEntity = (LmsAssignment) this;
        convertedEntity.setType(Assignment.class);
        convertedEntity.setId(getId());
        convertedEntity.setName(getName());
        convertedEntity.setPublished(isPublished());
        convertedEntity.setSecureParams(getSecureParams());
        convertedEntity.setDueAt(getDueAt());
        convertedEntity.setSubmissionTypes(getSubmissionTypes());
        convertedEntity.setPointsPossible(getPointsPossible());
        convertedEntity.setLockAt(getLockAt());
        convertedEntity.setUnlockAt(getUnlockAt());

        return convertedEntity;
    }

}
