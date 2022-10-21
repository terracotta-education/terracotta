package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = QuestionSubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionCommentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions";
    private static final Logger log = LoggerFactory.getLogger(QuestionSubmissionCommentController.class);

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private QuestionSubmissionCommentService questionSubmissionCommentService;

    @GetMapping("/{questionSubmissionId}/question_submission_comments")
    public ResponseEntity<List<QuestionSubmissionCommentDto>> getQuestionSubmissionComments(@PathVariable long experimentId,
                                                                                            @PathVariable long conditionId,
                                                                                            @PathVariable long treatmentId,
                                                                                            @PathVariable long assessmentId,
                                                                                            @PathVariable long submissionId,
                                                                                            @PathVariable long questionSubmissionId,
                                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<QuestionSubmissionCommentDto> questionSubmissionCommentList = questionSubmissionCommentService.getQuestionSubmissionComments(questionSubmissionId);

        if(questionSubmissionCommentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(questionSubmissionCommentList, HttpStatus.OK);
    }

    @GetMapping("/{questionSubmissionId}/question_submission_comments/{questionSubmissionCommentId}")
    public ResponseEntity<QuestionSubmissionCommentDto> getQuestionSubmissionComment(@PathVariable long experimentId,
                                                                                     @PathVariable long conditionId,
                                                                                     @PathVariable long treatmentId,
                                                                                     @PathVariable long assessmentId,
                                                                                     @PathVariable long submissionId,
                                                                                     @PathVariable long questionSubmissionId,
                                                                                     @PathVariable long questionSubmissionCommentId,
                                                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }


        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        QuestionSubmissionCommentDto questionSubmissionCommentDto = questionSubmissionCommentService.toDto(questionSubmissionCommentService.getQuestionSubmissionComment(questionSubmissionCommentId));

        return new ResponseEntity<>(questionSubmissionCommentDto, HttpStatus.OK);
    }

    @PostMapping("/{questionSubmissionId}/question_submission_comments")
    public ResponseEntity<QuestionSubmissionCommentDto> postQuestionSubmissionComment(@PathVariable long experimentId,
                                                                                      @PathVariable long conditionId,
                                                                                      @PathVariable long treatmentId,
                                                                                      @PathVariable long assessmentId,
                                                                                      @PathVariable long submissionId,
                                                                                      @PathVariable long questionSubmissionId,
                                                                                      @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                                      UriComponentsBuilder ucBuilder,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException, DataServiceException {

        log.debug("Creating question submission comment: {}", questionSubmissionCommentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        QuestionSubmissionCommentDto returnedDto = questionSubmissionCommentService.postQuestionSubmissionComment(questionSubmissionCommentDto, questionSubmissionId, securedInfo.getUserId());
        HttpHeaders headers = questionSubmissionCommentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, returnedDto.getQuestionSubmissionCommentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{questionSubmissionId}/question_submission_comments/{questionSubmissionCommentId}")
    public ResponseEntity<Void> updateQuestionSubmissionComment(@PathVariable long experimentId,
                                                                @PathVariable long conditionId,
                                                                @PathVariable long treatmentId,
                                                                @PathVariable long assessmentId,
                                                                @PathVariable long submissionId,
                                                                @PathVariable long questionSubmissionId,
                                                                @PathVariable long questionSubmissionCommentId,
                                                                @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, InvalidUserException, DataServiceException {

        log.debug("Updating question submission comment with id {}", questionSubmissionCommentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        questionSubmissionCommentService.updateQuestionSubmissionComment(questionSubmissionCommentDto, questionSubmissionCommentId, experimentId, submissionId, securedInfo.getUserId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{questionSubmissionId}/question_submission_comments/{questionSubmissionCommentId}")
    public ResponseEntity<Void> deleteQuestionSubmissionComment(@PathVariable long experimentId,
                                                                @PathVariable long conditionId,
                                                                @PathVariable long treatmentId,
                                                                @PathVariable long assessmentId,
                                                                @PathVariable long submissionId,
                                                                @PathVariable long questionSubmissionId,
                                                                @PathVariable long questionSubmissionCommentId,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            questionSubmissionCommentService.deleteById(questionSubmissionCommentId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
