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
import edu.iu.terracotta.model.app.dto.FileResponseDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerSubmissionController {

    /**
     * This controller was built to support the addition of answer submission types.
     */

    public static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    private AnswerSubmissionService answerSubmissionService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private APIJWTService apijwtService;

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AnswerSubmissionDto>> getAnswerSubmissionsByQuestionId(@PathVariable("experiment_id") Long experimentId,
                                                                                      @PathVariable("condition_id") Long conditionId,
                                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                                      @PathVariable("submission_id") Long submissionId,
                                                                                      @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException, IOException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
            List<AnswerSubmissionDto> answerSubmissionDtoList = answerSubmissionService.getAnswerSubmissions(questionSubmissionId, answerType);
            if(answerSubmissionDtoList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(answerSubmissionDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AnswerSubmissionDto> getAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                   @PathVariable("condition_id") Long conditionId,
                                                                   @PathVariable("treatment_id") Long treatmentId,
                                                                   @PathVariable("assessment_id") Long assessmentId,
                                                                   @PathVariable("submission_id") Long submissionId,
                                                                   @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                   @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException, IOException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            return new ResponseEntity<>(answerSubmissionService.getAnswerSubmission(answerSubmissionId, answerType), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/answer_submissions",
                    method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<List<AnswerSubmissionDto>> postAnswerSubmissions(
            @PathVariable("experiment_id") Long experimentId,
            @PathVariable("condition_id") Long conditionId,
            @PathVariable("treatment_id") Long treatmentId,
            @PathVariable("assessment_id") Long assessmentId,
            @PathVariable("submission_id") Long submissionId,
            @RequestBody List<AnswerSubmissionDto> answerSubmissionDtoList,
            HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException,
            QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException,
            TypeNotSupportedException, DataServiceException, IdMissingException, ExceedingLimitException, IOException {

        log.info("Creating answer submissions: {}", answerSubmissionDtoList);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        for (AnswerSubmissionDto answerSubmissionDto : answerSubmissionDtoList) {
            apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId,
                    answerSubmissionDto.getQuestionSubmissionId());
        }

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            List<AnswerSubmissionDto> returnedDtoList = answerSubmissionService
                    .postAnswerSubmissions(answerSubmissionDtoList);
            return new ResponseEntity<>(returnedDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    /*
    As other question types are added, it may be useful to add another request allowing for the PUT of a list of answer submissions.
    For example, a fill-in-the-blank question with multiple blanks to fill in.
    */
    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @PathVariable("submission_id") Long submissionId,
                                                       @PathVariable("question_submission_id") Long questionSubmissionId,
                                                       @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                       @RequestBody AnswerSubmissionDto answerSubmissionDto,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, AnswerNotMatchingException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            try{
                answerSubmissionService.updateAnswerSubmission(answerSubmissionDto, answerSubmissionId, answerType);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                throw new DataServiceException("Error 105: Unable to update answer submission: " + e.getMessage());
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @PathVariable("submission_id") Long submissionId,
                                                       @PathVariable("question_submission_id") Long questionSubmissionId,
                                                       @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            try{
                answerSubmissionService.deleteAnswerSubmission(answerSubmissionId, answerType);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (DataServiceException e) {
                throw new DataServiceException("Error 105: Could not delete answer submission. " + e.getMessage());
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value = "/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/answer_submissions/file",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<List<AnswerSubmissionDto>> postFileAnswerSubmission(@PathVariable long experimentId,
                                                                        @PathVariable long conditionId,
                                                                        @PathVariable long treatmentId,
                                                                        @PathVariable long assessmentId,
                                                                        @PathVariable long submissionId,
                                                                        @RequestParam("answer_dto") String answerSubmissionDtoStr,
                                                                        UriComponentsBuilder ucBuilder,
                                                                        @RequestPart("file") MultipartFile file,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException {

        if (file.isEmpty()) {
            log.error("Invalid file ");
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        AnswerSubmissionDto answerSubmissionDto = new ObjectMapper().readValue(answerSubmissionDtoStr, AnswerSubmissionDto.class);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, answerSubmissionDto.getQuestionSubmissionId());

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        log.info("Creating answer submission: {}", answerSubmissionDto);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        AnswerSubmissionDto returnedDto = answerSubmissionService.handleFileAnswerSubmission(answerSubmissionDto, file);
        HttpHeaders headers = answerSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId,
            answerSubmissionDto.getQuestionSubmissionId(), returnedDto.getAnswerSubmissionId());
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(returnedDto);

        return new ResponseEntity<>(answerSubmissionDtoList, headers, HttpStatus.OK);
    }

    @PutMapping(value = "/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/answer_submissions/{answerSubmissionId}/file",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<List<AnswerSubmissionDto>> putFileAnswerSubmission(@PathVariable long experimentId,
                                                                        @PathVariable long conditionId,
                                                                        @PathVariable long treatmentId,
                                                                        @PathVariable long assessmentId,
                                                                        @PathVariable long submissionId,
                                                                        @PathVariable long answerSubmissionId,
                                                                        @RequestParam("answer_dto") String answerSubmissionDtoStr,
                                                                        UriComponentsBuilder ucBuilder,
                                                                        @RequestPart("file") MultipartFile file,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException {

        if (file.isEmpty()) {
            log.error("Invalid file ");
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        AnswerSubmissionDto answerSubmissionDto = new ObjectMapper().readValue(answerSubmissionDtoStr, AnswerSubmissionDto.class);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, answerSubmissionDto.getQuestionSubmissionId());

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        log.info("Creating answer submission: {}", answerSubmissionDto);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        AnswerSubmissionDto returnedDto = answerSubmissionService.handleFileAnswerSubmissionUpdate(answerSubmissionDto, file);
        HttpHeaders headers = answerSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId,
            answerSubmissionDto.getQuestionSubmissionId(), returnedDto.getAnswerSubmissionId());
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(returnedDto);

        return new ResponseEntity<>(answerSubmissionDtoList, headers, HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}/file")
    public ResponseEntity<Resource> downloadFileAnswerSubmission(@PathVariable long experimentId,
                                                                        @PathVariable long conditionId,
                                                                        @PathVariable long treatmentId,
                                                                        @PathVariable long assessmentId,
                                                                        @PathVariable long submissionId,
                                                                        @PathVariable long questionSubmissionId,
                                                                        @PathVariable long answerSubmissionId,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, AnswerSubmissionNotMatchingException, IOException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        FileResponseDto fileResponseDto = answerSubmissionService.getFileResponseDto(answerSubmissionId);

        return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileResponseDto.getMimeType()))
                    .contentLength(fileResponseDto.getFile().length())
                    .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", fileResponseDto.getFileName()))
                    .body(new InputStreamResource(new FileInputStream(fileResponseDto.getFile())));
    }

}
