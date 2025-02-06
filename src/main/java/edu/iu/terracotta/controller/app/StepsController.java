package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.dto.StepDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
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

    @Autowired private ExposureService exposureService;
    @Autowired private ParticipantService participantService;
    @Autowired private GroupService groupService;
    @Autowired private SubmissionService submissionService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private QuestionSubmissionService questionSubmissionService;
    @Autowired private APIJWTService apijwtService;

    @PostMapping
    public ResponseEntity<Object> postStep(@PathVariable long experimentId,
                                           @RequestBody StepDto stepDto,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException,
            ParticipantNotUpdatedException, ExperimentStartedException, ConnectionException, CanvasApiException,
            IOException, AssignmentDatesException, AssessmentNotMatchingException, GroupNotMatchingException,
            ParticipantNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, IntegrationTokenNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        switch (stepDto.getStep()) {
            case EXPOSURE_TYPE: // We create the exposures.
                if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                    return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }

                exposureService.createExposures(experimentId);

                return new ResponseEntity<>(HttpStatus.OK);
            case PARTICIPATION_TYPE: //We prepare the participants with the right consent and consent related dates.
                if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                    return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }

                participantService.prepareParticipation(experimentId, securedInfo);

                return new ResponseEntity<>(HttpStatus.OK);
            case DISTRIBUTION_TYPE: //We prepare the groups once the distribution type is selected.
                if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                    return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }

                groupService.createAndAssignGroupsToConditionsAndExposures(experimentId, securedInfo, false);

                return new ResponseEntity<>(HttpStatus.OK);
            case STUDENT_SUBMISSION: //Mark the submission as finished and calculate the automatic grade.
                List<String> submissionsId;

                if (stepDto.getParameters() == null) {
                    return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
                }

                submissionsId = Collections.arrayToList(StringUtils.split(stepDto.getParameters().get("submissionIds"), ","));

                if (submissionsId.isEmpty()) {
                    return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
                }

                boolean student = apijwtService.isLearner(securedInfo) && !apijwtService.isInstructorOrHigher(securedInfo);

                try {
                    if (apijwtService.isLearner(securedInfo) && !apijwtService.isInstructorOrHigher(securedInfo)) {
                        if (submissionsId.size() > 1) {
                            return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
                        }

                        Long submissionId = Long.parseLong(submissionsId.get(0));
                        questionSubmissionService.canSubmit(securedInfo, experimentId);
                        submissionService.allowedSubmission(submissionId, securedInfo);
                        submissionService.finalizeAndGrade(submissionId, securedInfo, student);
                    } else if (apijwtService.isInstructorOrHigher(securedInfo)) {
                        for (String submissionIdString : submissionsId) {
                            Long submissionId = Long.parseLong(submissionIdString);
                            submissionService.finalizeAndGrade(submissionId, securedInfo, student);
                        }
                    } else {
                        return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                    }
                } catch (AssignmentAttemptException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
                }

                return new ResponseEntity<>(HttpStatus.OK);
            case POST_ASSIGNMENT:
                List<String> assignmentsId;

                if (stepDto.getParameters() == null) {
                    return new ResponseEntity<>(TextConstants.SUBMISSION_IDS_MISSING, HttpStatus.BAD_REQUEST);
                }

                assignmentsId = Collections.arrayToList(StringUtils.split(stepDto.getParameters().get("assignmentIds"), ","));

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

                    assignmentService.sendAssignmentGradeToCanvas(assignment.get());
                }

                return new ResponseEntity<>(HttpStatus.OK);
            case LAUNCH_ASSIGNMENT:
                if (!apijwtService.isLearner(securedInfo) || apijwtService.isInstructorOrHigher(securedInfo)) {
                    return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }

                try {
                    questionSubmissionService.canSubmit(securedInfo, experimentId);

                    return assignmentService.launchAssignment(experimentId, securedInfo);
                } catch (AssignmentAttemptException | AssignmentNotMatchingException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
                }
            case LAUNCH_CONSENT_ASSIGNMENT:
                if (apijwtService.isLearner(securedInfo) && !apijwtService.isInstructorOrHigher(securedInfo)) {
                    // Return this student's participant record, refreshing the list of participants if necessary
                    List<Participant> currentParticipantList = participantService.findAllByExperimentId(experimentId);
                    List<ParticipantDto> studentUserAsParticipant = participantService.getParticipants(currentParticipantList, experimentId, securedInfo.getUserId(), true, securedInfo);

                    if (studentUserAsParticipant.isEmpty()) {
                        participantService.refreshParticipants(experimentId, currentParticipantList);
                        studentUserAsParticipant = participantService.getParticipants(currentParticipantList, experimentId, securedInfo.getUserId(), true, securedInfo);
                    }

                    return new ResponseEntity<>(studentUserAsParticipant.get(0), HttpStatus.OK);
                }

                return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
            case VIEW_ASSIGNMENT:
                if (!apijwtService.isLearner(securedInfo) || apijwtService.isInstructorOrHigher(securedInfo)) {
                    return new ResponseEntity<>(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
                }

                try {
                    AssessmentDto assessmentDto = assessmentService.viewAssessment(experimentId, securedInfo);

                    return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
                } catch (Exception e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
                }

            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
