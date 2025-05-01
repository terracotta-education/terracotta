package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.assignment.Assignment;
import edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "assignment")
public class AssignmentExtended extends LmsAssignment {

    @Builder.Default private Assignment assignment = new Assignment();

    @Override
    public String getId() {
        return Long.toString(assignment.getId());
    }

    @Override
    public void setId(String id) {
        assignment.setId(Long.parseLong(id));
    }

    @Override
    public String getName() {
        return assignment.getName();
    }

    @Override
    public void setName(String name) {
        assignment.setName(name);
    }

    @Override
    public boolean isPublished() {
        return BooleanUtils.isTrue(assignment.isPublished());
    }

    @Override
    public void setPublished(boolean published) {
        assignment.setPublished(published);
    }

    @Override
    public Date getDueAt() {
        return assignment.getDueAt();
    }

    @Override
    public void setDueAt(Date dueAt) {
        assignment.setDueAt(dueAt);
    }

    @Override
    public List<String> getSubmissionTypes() {
        return assignment.getSubmissionTypes();
    }

    @Override
    public void setSubmissionTypes(List<String> submissionTypes) {
        assignment.setSubmissionTypes(submissionTypes);
    }

    @Override
    public Float getPointsPossible() {
        if (assignment.getPointsPossible() == null) {
            return null;
        }

        return assignment.getPointsPossible().floatValue();
    }

    @Override
    public void setPointsPossible(Float pointsPossible) {
        assignment.setPointsPossible(pointsPossible.doubleValue());
    }

    @Override
    public Date getLockAt() {
        return assignment.getLockAt();
    }

    @Override
    public void setLockAt(Date lockAt) {
        assignment.setLockAt(lockAt);
    }

    @Override
    public Date getUnlockAt() {
        return assignment.getUnlockAt();
    }

    @Override
    public void setUnlockAt(Date unlockAt) {
        assignment.setUnlockAt(unlockAt);
    }

    @Override
    public String getSecureParams() {
        return super.getSecureParams();
    }

    @Override
    public void setSecureParams(String secureParams) {
        super.setSecureParams(secureParams);
    }

    @Override
    public int getAllowedAttempts() {
        return super.getAllowedAttempts();
    }

    @Override
    public void setAllowedAttempts(int allowedAttempts) {
        super.setAllowedAttempts(allowedAttempts);
    }

    @Override
    public boolean isCanSubmit() {
        return super.isCanSubmit();
    }

    @Override
    public void setCanSubmit(boolean canSubmit) {
        super.setCanSubmit(canSubmit);
    }

    @Override
    public LmsExternalToolFields getLmsExternalToolFields() {
        ExternalToolTagAttribute externalToolTagAttribute = assignment.getExternalToolTagAttributes();

        if (externalToolTagAttribute == null) {
            return null;
        }

        return LmsExternalToolFields.builder()
            .url(externalToolTagAttribute.getUrl())
            .resourceLinkId(externalToolTagAttribute.getResourceLinkId())
            .build();
    }

    @Override
    public LmsAssignment from() {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setAllowedAttempts(getAllowedAttempts());
        lmsAssignment.setCanSubmit(isCanSubmit());
        lmsAssignment.setId(getId());
        lmsAssignment.setName(getName());
        lmsAssignment.setPublished(isPublished());
        lmsAssignment.setSecureParams(getSecureParams());
        lmsAssignment.setDueAt(getDueAt());
        lmsAssignment.setSubmissionTypes(getSubmissionTypes());
        lmsAssignment.setPointsPossible(getPointsPossible());
        lmsAssignment.setLockAt(getLockAt());
        lmsAssignment.setUnlockAt(getUnlockAt());
        lmsAssignment.setType(AssignmentExtended.class);
        lmsAssignment.setLmsExternalToolFields(getLmsExternalToolFields());

        return lmsAssignment;
    }

    public static AssignmentExtended of(LmsAssignment lmsAssignment) {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        if (lmsAssignment == null) {
            return assignmentExtended;
        }

        assignmentExtended.setAllowedAttempts(lmsAssignment.getAllowedAttempts());
        assignmentExtended.setCanSubmit(lmsAssignment.isCanSubmit());
        assignmentExtended.setDueAt(lmsAssignment.getDueAt());
        assignmentExtended.setId(lmsAssignment.getId());
        assignmentExtended.setLmsExternalToolFields(lmsAssignment.getLmsExternalToolFields());
        assignmentExtended.setLockAt(lmsAssignment.getLockAt());
        assignmentExtended.setName(lmsAssignment.getName());
        assignmentExtended.setPointsPossible(lmsAssignment.getPointsPossible());
        assignmentExtended.setPublished(lmsAssignment.isPublished());
        assignmentExtended.setSecureParams(lmsAssignment.getSecureParams());
        assignmentExtended.setSubmissionTypes(lmsAssignment.getSubmissionTypes());
        assignmentExtended.setUnlockAt(lmsAssignment.getUnlockAt());

        if (assignmentExtended.getAssignment().getExternalToolTagAttributes() == null) {
            assignmentExtended.getAssignment().setExternalToolTagAttributes(assignmentExtended.getAssignment().new ExternalToolTagAttribute());
        }

        assignmentExtended.getAssignment().getExternalToolTagAttributes().setUrl(lmsAssignment.getLmsExternalToolFields().getUrl());
        assignmentExtended.getAssignment().getExternalToolTagAttributes().setResourceLinkId(lmsAssignment.getLmsExternalToolFields().getResourceLinkId());

        return assignmentExtended;
    }

}
