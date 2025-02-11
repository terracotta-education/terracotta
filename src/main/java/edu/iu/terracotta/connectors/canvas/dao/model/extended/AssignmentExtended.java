package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.assignment.Assignment;
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
    public Date getDueAt() {
        return assignment.getDueAt();
    }

    @Override
    public List<String> getSubmissionTypes() {
        return assignment.getSubmissionTypes();
    }

    @Override
    public Float getPointsPossible() {
        return assignment.getPointsPossible().floatValue();
    }

    @Override
    public Date getLockAt() {
        return assignment.getLockAt();
    }

    @Override
    public Date getUnlockAt() {
        return assignment.getUnlockAt();
    }

    @Override
    public String getSecureParams() {
        return secureParams;
    }

    @Override
    public void setSecureParams(String secureParams) {
        this.secureParams = secureParams;
    }

    @Override
    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    @Override
    public void setAllowedAttempts(int allowedAttempts) {
        this.allowedAttempts = allowedAttempts;
    }

    @Override
    public boolean isCanSubmit() {
        return canSubmit;
    }

    @Override
    public void setCanSubmit(boolean canSubmit) {
        this.canSubmit = canSubmit;
    }

    @Override
    public LmsAssignment convert() {
        return (LmsAssignment) this;
    }

}
