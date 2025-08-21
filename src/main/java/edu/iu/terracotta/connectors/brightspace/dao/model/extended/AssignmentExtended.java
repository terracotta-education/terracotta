package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@SuperBuilder
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentExtended extends LmsAssignment {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.fffZ";

    @Builder.Default private Assignment assignment = Assignment.builder().build();

    @Override
    public String getId() {
        if (assignment.getDropboxFolder() == null) {
            return null;
        }

        return Long.toString(assignment.getDropboxFolder().getId());
    }

    @Override
    public void setId(String id) {
        if (StringUtils.isBlank(id) || assignment.getDropboxFolder() == null) {
            return;
        }

        assignment.getDropboxFolder().setId(Long.parseLong(id));
    }

    @Override
    public String getName() {
        if (assignment.getDropboxFolder() == null) {
            return null;
        }

        return assignment.getDropboxFolder().getName();
    }

    @Override
    public void setName(String name) {
        if (assignment.getDropboxFolder() == null) {
            return;
        }
        assignment.getDropboxFolder().setName(name);
    }

    @Override
    public boolean isPublished() {
        return assignment.getDropboxFolder() != null && BooleanUtils.isFalse(assignment.getDropboxFolder().getIsHidden());
    }

    @Override
    public void setPublished(boolean published) {
        if (assignment.getDropboxFolder() == null) {
            return;
        }

        assignment.getDropboxFolder().setIsHidden(!published);
    }

    @Override
    public Date getDueAt() {
        if (assignment.getDropboxFolder() == null) {
            return null;
        }

        return parseStringToDate(assignment.getDropboxFolder().getDueDate());
    }

    @Override
    public void setDueAt(Date dueAt) {
        if (assignment.getDropboxFolder() == null) {
            return;
        }

        assignment.getDropboxFolder().setDueDate(parseDateToString(dueAt));
    }

    @Override
    public List<String> getSubmissionTypes() {
        if (assignment.getDropboxFolder() == null) {
            return List.of();
        }

        return List.of(Integer.toString(assignment.getDropboxFolder().getSubmissionType()));
    }

    @Override
    public void setSubmissionTypes(List<String> submissionTypes) {
        if (CollectionUtils.isEmpty(submissionTypes)) {
            return;
        }

        assignment.getDropboxFolder().setSubmissionType(Integer.parseInt(submissionTypes.get(0)));
    }

    @Override
    public Float getPointsPossible() {
        if (assignment.getLineItem() == null) {
            return null;
        }

        return assignment.getLineItem().getScoreMaximum();
    }

    @Override
    public void setPointsPossible(Float pointsPossible) {
        if (pointsPossible == null) {
            assignment.getLineItem().setScoreMaximum(0F);
        }

        assignment.getLineItem().setScoreMaximum(pointsPossible);
    }

    @Override
    public Date getLockAt() {
        if (assignment.getDropboxFolder() == null || assignment.getDropboxFolder().getAvailability() == null) {
            return null;
        }

        return parseStringToDate(assignment.getDropboxFolder().getAvailability().getEndDate());
    }

    @Override
    public void setLockAt(Date lockAt) {
        if (assignment.getDropboxFolder() == null || assignment.getDropboxFolder().getAvailability() == null) {
            return;
        }

        assignment.getDropboxFolder().getAvailability().setEndDate(parseDateToString(lockAt));
    }

    @Override
    public Date getUnlockAt() {
        if (assignment.getDropboxFolder() == null || assignment.getDropboxFolder().getAvailability() == null) {
            return null;
        }

        return parseStringToDate(assignment.getDropboxFolder().getAvailability().getStartDate());
    }

    @Override
    public void setUnlockAt(Date unlockAt) {
        if (assignment.getDropboxFolder() == null || assignment.getDropboxFolder().getAvailability() == null) {
            return;
        }

        assignment.getDropboxFolder().getAvailability().setStartDate(parseDateToString(unlockAt));
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
        return null;
    }

    @Override
    public String getMetadata() {
        return super.getMetadata();
    }

    @Override
    public void setMetadata(String metadata) {
        super.setMetadata(metadata);
    }

    @Override
    public LmsAssignment from() {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();

        lmsAssignment.setAllowedAttempts(getAllowedAttempts());
        lmsAssignment.setCanSubmit(isCanSubmit());
        lmsAssignment.setDueAt(getDueAt());
        lmsAssignment.setId(getId());
        lmsAssignment.setLmsExternalToolFields(getLmsExternalToolFields());
        lmsAssignment.setLockAt(getLockAt());
        lmsAssignment.setMetadata(getMetadata());
        lmsAssignment.setName(getName());
        lmsAssignment.setPointsPossible(getPointsPossible());
        lmsAssignment.setPublished(isPublished());
        lmsAssignment.setSecureParams(getSecureParams());
        lmsAssignment.setSubmissionTypes(getSubmissionTypes());
        lmsAssignment.setType(AssignmentExtended.class);
        lmsAssignment.setUnlockAt(getUnlockAt());

        return lmsAssignment;
    }

    public static AssignmentExtended of(LmsAssignment lmsAssignment) {
        if (lmsAssignment == null) {
            return AssignmentExtended.builder().build();
        }

        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        assignmentExtended.setAllowedAttempts(lmsAssignment.getAllowedAttempts());
        assignmentExtended.setCanSubmit(lmsAssignment.isCanSubmit());
        assignmentExtended.setDueAt(lmsAssignment.getDueAt());
        assignmentExtended.setId(lmsAssignment.getId());
        assignmentExtended.setLmsExternalToolFields(lmsAssignment.getLmsExternalToolFields());
        assignmentExtended.setLockAt(lmsAssignment.getLockAt());
        assignmentExtended.setMetadata(lmsAssignment.getMetadata());
        assignmentExtended.setName(lmsAssignment.getName());
        assignmentExtended.setPointsPossible(lmsAssignment.getPointsPossible());
        assignmentExtended.setPublished(lmsAssignment.isPublished());
        assignmentExtended.setSecureParams(lmsAssignment.getSecureParams());
        assignmentExtended.setSubmissionTypes(lmsAssignment.getSubmissionTypes());
        assignmentExtended.setUnlockAt(lmsAssignment.getUnlockAt());

        return assignmentExtended;
    }

    private Date parseStringToDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }

        try {
            return Date.from(Instant.parse(date));
        } catch (Exception e) {
            log.error("Error parsing date: [{}]", date, e);
        }

        return null;
    }

    private String parseDateToString(Date date) {
        if (date == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

}
