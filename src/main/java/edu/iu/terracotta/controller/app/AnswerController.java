package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = AnswerController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerController {
    /**
     This controller was built to support the addition of question/answer types. The current state of the controller only supports the MC (multiple choice) type,
     but all of the requests can be updated with switch statements to support additional types.
     */

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions/{questionId}/answers";

    @Autowired private APIJWTService apijwtService;
    @Autowired private AnswerService answerService;
    @Autowired private QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<AnswerDto>> getAnswersByQuestion(@PathVariable long experimentId,
                                                                @PathVariable long conditionId,
                                                                @PathVariable long treatmentId,
                                                                @PathVariable long assessmentId,
                                                                @PathVariable long questionId,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (answerService.getQuestionType(questionId).equals(QuestionTypes.MC.toString())) {
            List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(questionId, false);

            if (answerDtoList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(answerDtoList, HttpStatus.OK);
        }

        return new ResponseEntity("Error 103: Answer type is not supported.", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<AnswerDto> getAnswer(@PathVariable long experimentId,
                                               @PathVariable long conditionId,
                                               @PathVariable long treatmentId,
                                               @PathVariable long assessmentId,
                                               @PathVariable long questionId,
                                               @PathVariable long answerId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerService.getQuestionType(questionId), answerId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String answerType = answerService.getQuestionType(questionId);

        if (answerType.equals(QuestionTypes.MC.toString())) {
            return new ResponseEntity<>(answerService.getAnswerMC(answerId), HttpStatus.OK);
        }

        return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
    }

    @PostMapping
    public ResponseEntity<AnswerDto> postAnswer(@PathVariable long experimentId,
                                                @PathVariable long conditionId,
                                                @PathVariable long treatmentId,
                                                @PathVariable long assessmentId,
                                                @PathVariable long questionId,
                                                @RequestBody AnswerDto answerDto,
                                                UriComponentsBuilder ucBuilder,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, MultipleChoiceLimitReachedException, IdInPostException, DataServiceException {
        log.debug("Creating Answer for question ID: {}", questionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AnswerDto returnedMcdDto = answerService.postAnswerMC(answerDto, questionId);
        HttpHeaders mcHeaders = answerService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, questionId, returnedMcdDto.getAnswerId());

        return new ResponseEntity<>(returnedMcdDto, mcHeaders, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<List<AnswerDto>> updateAnswers(@PathVariable long experimentId,
                                              @PathVariable long conditionId,
                                              @PathVariable long treatmentId,
                                              @PathVariable long assessmentId,
                                              @PathVariable long questionId,
                                              @RequestBody List<AnswerDto> answerDtoList,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException, DataServiceException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        String answerType = answerService.getQuestionType(questionId);

        if (!QuestionTypes.MC.toString().equals(answerType)) {
            return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
        }

        Map<AnswerMc, AnswerDto> map = new HashMap<>();

        for (AnswerDto answerDto : answerDtoList) {
            apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerDto.getAnswerId());
            AnswerMc mcAnswer = answerService.findByAnswerId(answerDto.getAnswerId());
            log.debug("Updating answer with id: {}", mcAnswer.getAnswerMcId());
            map.put(mcAnswer, answerDto);
        }

        try {
            return new ResponseEntity<>(answerService.updateAnswerMC(map), HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException(String.format("Error 105: An error occurred trying to update the answer list. No answers were updated. %s", ex.getMessage()), ex);
        }
    }

    @PutMapping("/{answerId}")
    public ResponseEntity<AnswerDto> updateAnswer(@PathVariable long experimentId,
                                             @PathVariable long conditionId,
                                             @PathVariable long treatmentId,
                                             @PathVariable long assessmentId,
                                             @PathVariable long questionId,
                                             @PathVariable long answerId,
                                             @RequestBody AnswerDto answerDto,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException, DataServiceException {
        log.debug("Updating answer with id: {}", answerId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        String answerType = questionService.findByQuestionId(questionId).getQuestionType().toString();
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!QuestionTypes.MC.toString().equals(answerType)) {
            return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
        }

        AnswerMc answerMc = answerService.findByAnswerId(answerId);
        Map<AnswerMc, AnswerDto> map = new HashMap<>();
        map.put(answerMc, answerDto);

        List<AnswerDto> answerDtos = answerService.updateAnswerMC(map);

        if (CollectionUtils.isEmpty(answerDtos)) {
            throw new DataServiceException("Error 105: An error occurred trying to update the answer. Answer was not updated.");
        }

        return new ResponseEntity<>(answerDtos.get(0), HttpStatus.OK);
    }

    @DeleteMapping("/{answerId}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable long experimentId,
                                             @PathVariable long conditionId,
                                             @PathVariable long treatmentId,
                                             @PathVariable long assessmentId,
                                             @PathVariable long questionId,
                                             @PathVariable long answerId,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        String answerType = questionService.findByQuestionId(questionId).getQuestionType().toString();
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!answerType.equals(QuestionTypes.MC.toString())) {
            return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
        }

        try {
            answerService.deleteByIdMC(answerId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
