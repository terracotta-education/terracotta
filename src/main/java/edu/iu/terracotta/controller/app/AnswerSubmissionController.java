package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerSubmissionController {

    /**
     This controller was built to support the addition of answer submission types.
     */

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}";
    private static final Logger log = LoggerFactory.getLogger(AnswerSubmissionController.class);

    @Autowired
    private AnswerSubmissionService answerSubmissionService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping("/question_submissions/{question_submissionId}/answer_submissions")
    public ResponseEntity<List<AnswerSubmissionDto>> getAnswerSubmissionsByQuestionId(@PathVariable long experimentId,
                                                                                      @PathVariable long conditionId,
                                                                                      @PathVariable long treatmentId,
                                                                                      @PathVariable long assessmentId,
                                                                                      @PathVariable long submissionId,
                                                                                      @PathVariable long questionSubmissionId,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = answerSubmissionService.getAnswerSubmissions(questionSubmissionId, answerType);

        if(answerSubmissionDtoList.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(answerSubmissionDtoList, HttpStatus.OK);
    }


    @GetMapping("/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}")
    public ResponseEntity<AnswerSubmissionDto> getAnswerSubmission(@PathVariable long experimentId,
                                                                   @PathVariable long conditionId,
                                                                   @PathVariable long treatmentId,
                                                                   @PathVariable long assessmentId,
                                                                   @PathVariable long submissionId,
                                                                   @PathVariable long questionSubmissionId,
                                                                   @PathVariable long answerSubmissionId,
                                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(!apijwtService.isLearnerOrHigher(securedInfo)){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        return new ResponseEntity<>(answerSubmissionService.getAnswerSubmission(answerSubmissionId, answerType), HttpStatus.OK);
    }


    @PostMapping("/answer_submissions")
    @Transactional
    public ResponseEntity<List<AnswerSubmissionDto>> postAnswerSubmissions(
            @PathVariable long experimentId,
            @PathVariable long conditionId,
            @PathVariable long treatmentId,
            @PathVariable long assessmentId,
            @PathVariable long submissionId,
            @RequestBody List<AnswerSubmissionDto> answerSubmissionDtoList,
            HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException,
            QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException,
            TypeNotSupportedException, DataServiceException, IdMissingException, ExceedingLimitException {

        log.info("Creating answer submissions: {}", answerSubmissionDtoList);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        for (AnswerSubmissionDto answerSubmissionDto : answerSubmissionDtoList) {
            apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId,
                    answerSubmissionDto.getQuestionSubmissionId());
        }

        if(!apijwtService.isLearnerOrHigher(securedInfo)){
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<AnswerSubmissionDto> returnedDtoList = answerSubmissionService
                .postAnswerSubmissions(answerSubmissionDtoList);

        return new ResponseEntity<>(returnedDtoList, HttpStatus.OK);
    }

    /*
    As other question types are added, it may be useful to add another request allowing for the PUT of a list of answer submissions.
    For example, a fill-in-the-blank question with multiple blanks to fill in.
    */
    @PutMapping("/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}")
    public ResponseEntity<Void> updateAnswerSubmission(@PathVariable long experimentId,
                                                       @PathVariable long conditionId,
                                                       @PathVariable long treatmentId,
                                                       @PathVariable long assessmentId,
                                                       @PathVariable long submissionId,
                                                       @PathVariable long questionSubmissionId,
                                                       @PathVariable long answerSubmissionId,
                                                       @RequestBody AnswerSubmissionDto answerSubmissionDto,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, AnswerNotMatchingException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)){
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        try {
            answerSubmissionService.updateAnswerSubmission(answerSubmissionDto, answerSubmissionId, answerType);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            throw new DataServiceException("Error 105: Unable to update answer submission: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}")
    public ResponseEntity<Void> deleteAnswerSubmission(@PathVariable long experimentId,
                                                       @PathVariable long conditionId,
                                                       @PathVariable long treatmentId,
                                                       @PathVariable long assessmentId,
                                                       @PathVariable long submissionId,
                                                       @PathVariable long questionSubmissionId,
                                                       @PathVariable long answerSubmissionId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            answerSubmissionService.deleteAnswerSubmission(answerSubmissionId, answerType);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Could not delete answer submission. " + e.getMessage(), e);
        }
    }

}
