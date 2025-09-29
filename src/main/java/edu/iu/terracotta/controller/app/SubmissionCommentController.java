package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = SubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class SubmissionCommentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/submission_comments";

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ApiJwtService apijwtService;
    @Autowired private SubmissionService submissionService;
    @Autowired private SubmissionCommentService submissionCommentService;

    @GetMapping
    public ResponseEntity<List<SubmissionCommentDto>> getSubmissionCommentsBySubmission(@PathVariable long experimentId,
                                                                                        @PathVariable long conditionId,
                                                                                        @PathVariable long treatmentId,
                                                                                        @PathVariable long assessmentId,
                                                                                        @PathVariable long submissionId,
                                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<SubmissionCommentDto> submissionCommentList = submissionCommentService.getSubmissionComments(submissionId);

        if (submissionCommentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(submissionCommentList, HttpStatus.OK);
    }

    @GetMapping("/{submissionCommentId}")
    public ResponseEntity<SubmissionCommentDto> getSubmissionComment(@PathVariable long experimentId,
                                                                     @PathVariable long conditionId,
                                                                     @PathVariable long treatmentId,
                                                                     @PathVariable long assessmentId,
                                                                     @PathVariable long submissionId,
                                                                     @PathVariable long submissionCommentId,
                                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException, InvalidUserException, NumberFormatException, TerracottaConnectorException{
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        SubmissionCommentDto submissionCommentDto = submissionCommentService.toDto(submissionCommentService.getSubmissionComment(submissionCommentId));

        return new ResponseEntity<>(submissionCommentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<SubmissionCommentDto> postSubmissionComment(@PathVariable long experimentId,
                                                                      @PathVariable long conditionId,
                                                                      @PathVariable long treatmentId,
                                                                      @PathVariable long assessmentId,
                                                                      @PathVariable long submissionId,
                                                                      @RequestBody SubmissionCommentDto submissionCommentDto,
                                                                      UriComponentsBuilder ucBuilder,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        log.debug("Creating submission comment for submission ID: {}", submissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        SubmissionCommentDto returnedDto = submissionCommentService.postSubmissionComment(submissionCommentDto, submissionId, securedInfo);
        HttpHeaders headers = submissionCommentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, returnedDto.getSubmissionCommentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{submissionCommentId}")
    public ResponseEntity<Void> updateSubmissionComment(@PathVariable long experimentId,
                                                        @PathVariable long conditionId,
                                                        @PathVariable long treatmentId,
                                                        @PathVariable long assessmentId,
                                                        @PathVariable long submissionId,
                                                        @PathVariable long submissionCommentId,
                                                        @RequestBody SubmissionCommentDto submissionCommentDto,
                                                        HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        log.debug("Updating submission comment with id {}", submissionCommentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        SubmissionComment submissionComment = submissionCommentService.getSubmissionComment(submissionCommentId);
        LtiUserEntity user = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (!user.getDisplayName().equals(submissionComment.getCreator())) {
            return new ResponseEntity("Error 122: Only the creator of a comment can edit their own comment.", HttpStatus.UNAUTHORIZED);
        }

        submissionCommentService.updateSubmissionComment(submissionComment, submissionCommentDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{submissionCommentId}")
    public ResponseEntity<Void> deleteSubmissionComment(@PathVariable long experimentId,
                                                        @PathVariable long conditionId,
                                                        @PathVariable long treatmentId,
                                                        @PathVariable long assessmentId,
                                                        @PathVariable long submissionId,
                                                        @PathVariable long submissionCommentId,
                                                        HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            submissionCommentService.deleteById(submissionCommentId);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
