package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerSubmissionDto;
import edu.iu.terracotta.dao.model.dto.FileResponseDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerSubmissionController {

    /**
     * This controller was built to support the addition of answer submission types.
     */

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}";

    private final AnswerSubmissionService answerSubmissionService;
    private final SubmissionService submissionService;
    private final QuestionSubmissionService questionSubmissionService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ConditionService conditionService;
    private final TreatmentService treatmentService;
    private final AssessmentService assessmentService;

    @GetMapping("/question_submissions/{questionSubmissionId}/answer_submissions")
    public ResponseEntity<List<AnswerSubmissionDto>> getAnswerSubmissionsByQuestionId(@PathVariable("experimentId") UUID experimentUuid,
                                                                                      @PathVariable("conditionId") UUID conditionUuid,
                                                                                      @PathVariable("treatmentId") UUID treatmentUuid,
                                                                                      @PathVariable("assessmentId") UUID assessmentUuid,
                                                                                      @PathVariable("submissionId") UUID submissionUuid,
                                                                                      @PathVariable("questionSubmissionId") UUID questionSubmissionUuid,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, DataServiceException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long questionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(questionSubmissionUuid).getQuestionSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        List<AnswerSubmissionDto> answerSubmissionDtoList = answerSubmissionService.getAnswerSubmissions(questionSubmissionId, answerType);

        if (answerSubmissionDtoList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(answerSubmissionDtoList, HttpStatus.OK);
    }

    @GetMapping("/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}")
    public ResponseEntity<AnswerSubmissionDto> getAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                                   @PathVariable("conditionId") UUID conditionUuid,
                                                                   @PathVariable("treatmentId") UUID treatmentUuid,
                                                                   @PathVariable("assessmentId") UUID assessmentUuid,
                                                                   @PathVariable("submissionId") UUID submissionUuid,
                                                                   @PathVariable("questionSubmissionId") UUID questionSubmissionUuid,
                                                                   @PathVariable("answerSubmissionId") UUID answerSubmissionUuid,
                                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, DataServiceException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long questionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(questionSubmissionUuid).getQuestionSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        long answerSubmissionId = answerSubmissionService.resolveAnswerSubmissionId(answerSubmissionUuid, answerType);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        return new ResponseEntity<>(answerSubmissionService.getAnswerSubmission(answerSubmissionId, answerType), HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/answer_submissions")
    public ResponseEntity<List<AnswerSubmissionDto>> postAnswerSubmissions(@PathVariable("experimentId") UUID experimentUuid,
                                                                        @PathVariable("conditionId") UUID conditionUuid,
                                                                        @PathVariable("treatmentId") UUID treatmentUuid,
                                                                        @PathVariable("assessmentId") UUID assessmentUuid,
                                                                        @PathVariable("submissionId") UUID submissionUuid,
                                                                        @RequestBody List<AnswerSubmissionDto> answerSubmissionDtoList,
                                                                        HttpServletRequest req)
                                                                        throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException,
                                                                        QuestionSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, IdInPostException,
                                                                        TypeNotSupportedException, DataServiceException, IdMissingException, ExceedingLimitException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        log.info("Creating answer submissions for submission ID: {}", submissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        for (AnswerSubmissionDto answerSubmissionDto : answerSubmissionDtoList) {
            long answerSubmissionQuestionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(answerSubmissionDto.getQuestionSubmissionId()).getQuestionSubmissionId();
            apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId,
                    answerSubmissionQuestionSubmissionId);
        }

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<AnswerSubmissionDto> returnedDtoList = answerSubmissionService.postAnswerSubmissions(answerSubmissionDtoList);

        return new ResponseEntity<>(returnedDtoList, HttpStatus.OK);
    }

    /*
    As other question types are added, it may be useful to add another request allowing for the PUT of a list of answer submissions.
    For example, a fill-in-the-blank question with multiple blanks to fill in.
    */
    @PutMapping("/question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}")
    public ResponseEntity<Void> updateAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                       @PathVariable("conditionId") UUID conditionUuid,
                                                       @PathVariable("treatmentId") UUID treatmentUuid,
                                                       @PathVariable("assessmentId") UUID assessmentUuid,
                                                       @PathVariable("submissionId") UUID submissionUuid,
                                                       @PathVariable("questionSubmissionId") UUID questionSubmissionUuid,
                                                       @PathVariable("answerSubmissionId") UUID answerSubmissionUuid,
                                                       @RequestBody AnswerSubmissionDto answerSubmissionDto,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, AnswerNotMatchingException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long questionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(questionSubmissionUuid).getQuestionSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        long answerSubmissionId = answerSubmissionService.resolveAnswerSubmissionId(answerSubmissionUuid, answerType);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
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
    public ResponseEntity<Void> deleteAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                       @PathVariable("conditionId") UUID conditionUuid,
                                                       @PathVariable("treatmentId") UUID treatmentUuid,
                                                       @PathVariable("assessmentId") UUID assessmentUuid,
                                                       @PathVariable("submissionId") UUID submissionUuid,
                                                       @PathVariable("questionSubmissionId") UUID questionSubmissionUuid,
                                                       @PathVariable("answerSubmissionId") UUID answerSubmissionUuid,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long questionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(questionSubmissionUuid).getQuestionSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        long answerSubmissionId = answerSubmissionService.resolveAnswerSubmissionId(answerSubmissionUuid, answerType);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
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

    @PostMapping(value = "/answer_submissions/file", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<List<AnswerSubmissionDto>> postFileAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                                            @PathVariable("conditionId") UUID conditionUuid,
                                                                            @PathVariable("treatmentId") UUID treatmentUuid,
                                                                            @PathVariable("assessmentId") UUID assessmentUuid,
                                                                            @PathVariable("submissionId") UUID submissionUuid,
                                                                            @RequestParam("answer_dto") String answerSubmissionDtoStr,
                                                                            UriComponentsBuilder ucBuilder,
                                                                            @RequestPart("file") MultipartFile file,
                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();

        if (file.isEmpty()) {
            log.error("Invalid (empty) file for submission ID: [{}], experiment ID: [{}]", submissionId, experimentId);
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        AnswerSubmissionDto answerSubmissionDto = JsonMapper.builder()
            .build()
            .readValue(
                answerSubmissionDtoStr,
                AnswerSubmissionDto.class
            );
        long answerSubmissionQuestionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(answerSubmissionDto.getQuestionSubmissionId()).getQuestionSubmissionId();
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, answerSubmissionQuestionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        log.info("Creating answer submission: {}", answerSubmissionDto);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        AnswerSubmissionDto returnedDto = answerSubmissionService.handleFileAnswerSubmission(answerSubmissionDto, file);
        HttpHeaders headers = answerSubmissionService.buildHeaders(ucBuilder, experimentUuid, conditionUuid, treatmentUuid, assessmentUuid, submissionUuid,
            answerSubmissionDto.getQuestionSubmissionId(), returnedDto.getAnswerSubmissionId());
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(returnedDto);

        return new ResponseEntity<>(answerSubmissionDtoList, headers, HttpStatus.OK);
    }

    @PutMapping("/answer_submissions/{answerSubmissionId}/file")
    public ResponseEntity<List<AnswerSubmissionDto>> putFileAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                                            @PathVariable("conditionId") UUID conditionUuid,
                                                                            @PathVariable("treatmentId") UUID treatmentUuid,
                                                                            @PathVariable("assessmentId") UUID assessmentUuid,
                                                                            @PathVariable("submissionId") UUID submissionUuid,
                                                                            @PathVariable("answerSubmissionId") UUID answerSubmissionUuid,
                                                                            @RequestParam("answer_dto") String answerSubmissionDtoStr,
                                                                            UriComponentsBuilder ucBuilder,
                                                                            @RequestPart("file") MultipartFile file,
                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        if (file.isEmpty()) {
            log.error("Invalid (empty) file for submission ID: [{}], experiment ID: [{}]", submissionId, experimentId);
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        AnswerSubmissionDto answerSubmissionDto = JsonMapper.builder()
            .build()
            .readValue(
                answerSubmissionDtoStr,
                AnswerSubmissionDto.class
            );
        long answerSubmissionQuestionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(answerSubmissionDto.getQuestionSubmissionId()).getQuestionSubmissionId();
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, answerSubmissionQuestionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(answerSubmissionQuestionSubmissionId);
        // the resolved id here isn't otherwise consumed by this endpoint (mirrors the pre-existing
        // handling below, which never read the path's answerSubmissionId either), but resolving it
        // validates that the uuid corresponds to a real answer submission of this type.
        long answerSubmissionId = answerSubmissionService.resolveAnswerSubmissionId(answerSubmissionUuid, answerType);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        log.info("Creating answer submission: {}", answerSubmissionDto);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        AnswerSubmissionDto returnedDto = answerSubmissionService.handleFileAnswerSubmissionUpdate(answerSubmissionDto, file);
        HttpHeaders headers = answerSubmissionService.buildHeaders(ucBuilder, experimentUuid, conditionUuid, treatmentUuid, assessmentUuid, submissionUuid,
            answerSubmissionDto.getQuestionSubmissionId(), returnedDto.getAnswerSubmissionId());
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(returnedDto);

        return new ResponseEntity<>(answerSubmissionDtoList, headers, HttpStatus.OK);
    }

    @GetMapping("question_submissions/{questionSubmissionId}/answer_submissions/{answerSubmissionId}/file")
    public ResponseEntity<Resource> downloadFileAnswerSubmission(@PathVariable("experimentId") UUID experimentUuid,
                                                                        @PathVariable("conditionId") UUID conditionUuid,
                                                                        @PathVariable("treatmentId") UUID treatmentUuid,
                                                                        @PathVariable("assessmentId") UUID assessmentUuid,
                                                                        @PathVariable("submissionId") UUID submissionUuid,
                                                                        @PathVariable("questionSubmissionId") UUID questionSubmissionUuid,
                                                                        @PathVariable("answerSubmissionId") UUID answerSubmissionUuid,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, SubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, AnswerSubmissionNotMatchingException, IOException, NumberFormatException, TerracottaConnectorException {
        long submissionId = submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId();
        long questionSubmissionId = questionSubmissionService.getQuestionSubmissionByUuid(questionSubmissionUuid).getQuestionSubmissionId();
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        long answerSubmissionId = answerSubmissionService.resolveAnswerSubmissionId(answerSubmissionUuid, answerType);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        FileResponseDto fileResponseDto = answerSubmissionService.getFileResponseDto(answerSubmissionId);

        return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileResponseDto.getMimeType()))
                    .contentLength(fileResponseDto.getFile().length())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileResponseDto.getFileName(), StandardCharsets.UTF_8).build().toString())
                    .body(new InputStreamResource(new FileInputStream(fileResponseDto.getFile())));
    }

}
