package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.dao.model.dto.LmsUserBatchStatusDto;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.dao.model.dto.StepDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.async.ParticipantAsyncService;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@SuppressWarnings({"unchecked"})
@RequestMapping(value = StepsController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class StepsController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/step";
    public static final String EXPOSURE_TYPE = "exposure_type";
    public static final String PARTICIPATION_TYPE = "participation_type";
    public static final String DISTRIBUTION_TYPE = "distribution_type";
    public static final String STUDENT_SUBMISSION = "student_submission";
    public static final String POST_ASSIGNMENT = "post_assignment";
    public static final String LAUNCH_ASSIGNMENT = "launch_assignment";
    public static final String LAUNCH_CONSENT_ASSIGNMENT = "launch_consent_assignment";
    public static final String VIEW_ASSIGNMENT = "view_assignment";

    private final ExposureService exposureService;
    private final ParticipantService participantService;
    private final ParticipantAsyncService participantAsyncService;
    private final GroupService groupService;
    private final SubmissionService submissionService;
    private final AssessmentService assessmentService;
    private final AssignmentService assignmentService;
    private final QuestionSubmissionService questionSubmissionService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;

    @PostMapping
    public ResponseEntity<Object> postStep(@PathVariable("experimentId") UUID experimentUuid,
                                            @RequestParam(name = "preferLmsChecks", defaultValue = "false") boolean preferLmsChecks,
                                           @RequestBody StepDto stepDto,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        return switch (stepDto.getStep()) {
            case EXPOSURE_TYPE -> handleExposureType(experimentId, securedInfo);
            case PARTICIPATION_TYPE -> handleParticipationType(experimentId, securedInfo);
            case DISTRIBUTION_TYPE -> handleDistributionType(experimentId, securedInfo);
            case STUDENT_SUBMISSION -> handleStudentSubmission(experimentId, securedInfo, stepDto, preferLmsChecks);
            case POST_ASSIGNMENT -> handlePostAssignment(securedInfo, stepDto);
            case LAUNCH_ASSIGNMENT -> handleLaunchAssignment(experimentId, securedInfo, preferLmsChecks);
            case LAUNCH_CONSENT_ASSIGNMENT -> handleLaunchConsentAssignment(experimentId, securedInfo);
            case VIEW_ASSIGNMENT -> handleViewAssignment(experimentId, securedInfo);
            default -> new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        };
    }

    // We create the exposures.
    private ResponseEntity<Object> handleExposureType(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        exposureService.createExposures(experimentId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // We prepare the participants with the right consent and consent related dates.
    private ResponseEntity<Object> handleParticipationType(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        // refreshParticipants can take several minutes for a large course roster, so
        // kick it off in the background and hand back a status ID instead of blocking
        // this request (and holding its DB transaction open) for the whole duration -
        // unless the roster isn't due for a sync, in which case this already ran
        // synchronously and reports COMPLETED directly
        LmsUserBatchStatusDto lmsUserBatchStatusDto = participantService.startPrepareParticipation(experimentId, securedInfo);

        if (lmsUserBatchStatusDto.getStatus() == LmsUserBatchStatus.IN_PROGRESS) {
            participantAsyncService.prepareParticipationAsync(experimentId, securedInfo, lmsUserBatchStatusDto.getBatchId());
        }

        return new ResponseEntity<>(lmsUserBatchStatusDto, HttpStatus.OK);
    }

    // We prepare the groups once the distribution type is selected.
    private ResponseEntity<Object> handleDistributionType(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        groupService.createAndAssignGroupsToConditionsAndExposures(experimentId, securedInfo, false);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Mark the submission as finished and calculate the automatic grade.
    private ResponseEntity<Object> handleStudentSubmission(long experimentId, SecuredInfo securedInfo, StepDto stepDto, boolean preferLmsChecks)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (stepDto.getParameters() == null) {
            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
        }

        List<String> submissionsId = Collections.arrayToList(StringUtils.split(stepDto.getParameters().get("submissionIds"), ","));

        if (submissionsId.isEmpty()) {
            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
        }

        boolean instructorOrHigher = apijwtService.isInstructorOrHigher(securedInfo);

        if (!instructorOrHigher && submissionsId.size() > 1) {
            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
        }

        // a caller finalizing exactly one submission that belongs to them is always treated as
        // their own student submission, even if they also hold an elevated LTI role (e.g. a
        // Canvas institution Administrator) alongside their course enrollment - only an
        // elevated-role caller finalizing someone else's submission(s) goes through the
        // instructor batch path below
        boolean student = apijwtService.isLearner(securedInfo) && submissionsId.size() == 1
            && submissionService.isOwnSubmission(submissionService.getSubmissionIdByUuid(UUID.fromString(submissionsId.get(0))), securedInfo);

        try {
            if (student) {
                Long submissionId = submissionService.getSubmissionIdByUuid(UUID.fromString(submissionsId.get(0)));
                questionSubmissionService.canSubmit(securedInfo, experimentId, preferLmsChecks);
                submissionService.allowedSubmission(submissionId, securedInfo);
                submissionService.finalizeAndGrade(submissionId, securedInfo, student);
            } else if (instructorOrHigher) {
                for (String submissionIdString : submissionsId) {
                    Long submissionId = submissionService.getSubmissionIdByUuid(UUID.fromString(submissionIdString));
                    submissionService.finalizeAndGrade(submissionId, securedInfo, student);
                }
            } else {
                return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
            }
        } catch (AssignmentAttemptException | AssignmentLockedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<Object> handlePostAssignment(SecuredInfo securedInfo, StepDto stepDto)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (stepDto.getParameters() == null) {
            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
        }

        List<String> assignmentsId = Collections.arrayToList(StringUtils.split(stepDto.getParameters().get("assignmentIds"), ","));

        if (assignmentsId.isEmpty()) {
            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        for (String assignmentIdString : assignmentsId) {
            Long assignmentId = Long.parseLong(assignmentIdString);
            Optional<Assignment> assignment = assignmentService.findById(assignmentId);

            if (assignment.isEmpty()) {
                return new ResponseEntity<>(TextConstants.ASSIGNMENT_NOT_MATCHING + " : " + assignmentId, HttpStatus.NOT_FOUND);
            }

            assignmentService.sendAssignmentGradeToLms(assignment.get());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // any Learner-role holder may launch their own assignment, regardless of whatever
    // other elevated roles (e.g. Canvas institution Administrator) they also hold
    private ResponseEntity<Object> handleLaunchAssignment(long experimentId, SecuredInfo securedInfo, boolean preferLmsChecks)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isLearner(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            questionSubmissionService.canSubmit(securedInfo, experimentId, preferLmsChecks);

            return assignmentService.launchAssignment(experimentId, securedInfo);
        } catch (AssignmentAttemptException | AssignmentNotMatchingException | AssignmentLockedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    // any Learner-role holder may fetch/create their own participant record,
    // regardless of whatever other elevated roles they also hold
    private ResponseEntity<Object> handleLaunchConsentAssignment(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isLearner(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        // Return this student's participant record, creating it from the current
        // launch if it doesn't exist yet
        List<ParticipantDto> studentUserAsParticipant = participantService.getParticipants(experimentId, securedInfo.getUserId(), true, securedInfo, false);

        if (studentUserAsParticipant.isEmpty()) {
            participantService.ensureParticipantExists(experimentId, securedInfo);
            studentUserAsParticipant = participantService.getParticipants(experimentId, securedInfo.getUserId(), true, securedInfo, false);
        }

        return new ResponseEntity<>(studentUserAsParticipant.get(0), HttpStatus.OK);
    }

    // any Learner-role holder may view their own assignment, regardless of whatever
    // other elevated roles (e.g. Canvas institution Administrator) they also hold
    private ResponseEntity<Object> handleViewAssignment(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, ApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        if (!apijwtService.isLearner(securedInfo)) {
            return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            AssessmentDto assessmentDto = assessmentService.viewAssessment(experimentId, securedInfo);

            return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/status/{batchId}")
    public ResponseEntity<Object> getStepStatus(@PathVariable("experimentId") UUID experimentUuid, @PathVariable UUID batchId, HttpServletRequest req) throws BadTokenException, ExperimentNotMatchingException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        return participantService.getPrepareParticipationStatus(batchId)
            .map(lmsUserBatchStatusDto -> new ResponseEntity<>((Object) lmsUserBatchStatusDto, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
