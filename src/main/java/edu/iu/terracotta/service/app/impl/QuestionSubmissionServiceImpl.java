package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.AnswerEssaySubmission;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerSubmissionDto;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@SuppressWarnings({"squid:S2229", "PMD.GuardLogStatement"})
public class QuestionSubmissionServiceImpl implements QuestionSubmissionService {

    @Autowired private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private AnswerMcRepository answerMcRepository;
    @Autowired private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private AnswerService answerService;
    @Autowired private AnswerSubmissionService answerSubmissionService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private QuestionSubmissionCommentService questionSubmissionCommentService;
    @Autowired private ApiClient apiClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<QuestionSubmissionDto> getQuestionSubmissions(long submissionId, boolean answerSubmissions, boolean questionSubmissionComments, long assessmentId, boolean isStudent) throws AssessmentNotMatchingException, IOException {
        boolean showCorrectAnswers = true;

        if (isStudent) {
            Optional<Assessment> assessment = assessmentRepository.findById(assessmentId);

            if (assessment.isEmpty()) {
                throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
            }

            Submission submission = submissionRepository.findBySubmissionId(submissionId);
            boolean hasSubmitted = submission.getDateSubmitted() != null;
            // allow answerSubmissions if submission is unsubmitted (student users are
            // allowed to retrieve their previously saved answers in order, in the future,
            // to update and save responses)
            answerSubmissions = assessment.get().canViewResponses() || !hasSubmitted;
            questionSubmissionComments = answerSubmissions;
            // only allow returning correct answers for a submission that has been submitted
            showCorrectAnswers = assessment.get().canViewCorrectAnswers() && hasSubmitted;
        }

        List<QuestionSubmission> questionSubmissions = questionSubmissionRepository.findBySubmission_SubmissionId(submissionId);
        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();

        for (QuestionSubmission questionSubmission : questionSubmissions) {
            questionSubmissionDtoList.add(toDto(questionSubmission, answerSubmissions, questionSubmissionComments, showCorrectAnswers));
        }

        return questionSubmissionDtoList;
    }

    @Override
    public QuestionSubmission getQuestionSubmission(Long id) {
        return questionSubmissionRepository.findByQuestionSubmissionId(id);
    }

    @Override
    @Transactional
    // this method isn't technically fully transactional. The dto is validated beforehand.
    public void updateQuestionSubmissions(Map<QuestionSubmission, QuestionSubmissionDto> map, boolean student) throws InvalidUserException, DataServiceException, IdMissingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, AnswerNotMatchingException {
        for (Map.Entry<QuestionSubmission, QuestionSubmissionDto> entry : map.entrySet()) {
            QuestionSubmission questionSubmission = entry.getKey();
            QuestionSubmissionDto questionSubmissionDto = entry.getValue();

            if (questionSubmissionDto.getAlteredGrade() != null && student) {
                throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
            }

            questionSubmission.setAlteredGrade(questionSubmissionDto.getAlteredGrade());
            save(questionSubmission);

            for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                if (QuestionTypes.MC.equals(questionSubmission.getQuestion().getQuestionType())) {
                    answerSubmissionService.updateAnswerMcSubmission(answerSubmissionDto.getAnswerSubmissionId(), answerSubmissionDto);
                } else if (QuestionTypes.ESSAY.equals(questionSubmission.getQuestion().getQuestionType())) {
                    answerSubmissionService.updateAnswerEssaySubmission(answerSubmissionDto.getAnswerSubmissionId(), answerSubmissionDto);
                }
            }
        }
    }

    @Override
    @Transactional
    // this method isn't technically fully transactional. The dto is validated beforehand.
    public List<QuestionSubmissionDto> postQuestionSubmissions(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws DataServiceException {
        List<QuestionSubmissionDto> returnedDtoList = new ArrayList<>();
        log.debug("Creating {} question submissions for submission ID: [{}]", questionSubmissionDtoList.size(), submissionId);

        try {
            for (QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                questionSubmissionDto.setSubmissionId(submissionId);
                QuestionSubmission questionSubmission;
                questionSubmission = fromDto(questionSubmissionDto);
                returnedDtoList.add(toDto(save(questionSubmission), false, false));

                for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                    answerSubmissionDto.setQuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
                    answerSubmissionService.postAnswerSubmission(answerSubmissionDto, questionSubmission.getQuestionSubmissionId());
                }
            }

        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There was an error while creating the question submissions. No question submissions or answer submissions were created: " + ex.getMessage(), ex);
        }

        return returnedDtoList;
    }

    private QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments, boolean showCorrectAnswers) throws IOException {
        QuestionSubmissionDto questionSubmissionDto = toDto(questionSubmission, answerSubmissions, questionSubmissionComments);
        questionSubmissionDto.setAnswerDtoList(answerService.findAllByQuestionIdMC(questionSubmission.getQuestion().getQuestionId(), showCorrectAnswers));

        return questionSubmissionDto;
    }

    @Override
    public QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments) throws IOException {
        QuestionSubmissionDto questionSubmissionDto = new QuestionSubmissionDto();
        questionSubmissionDto.setQuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
        questionSubmissionDto.setSubmissionId(questionSubmission.getSubmission().getSubmissionId());
        questionSubmissionDto.setQuestionId(questionSubmission.getQuestion().getQuestionId());
        questionSubmissionDto.setCalculatedPoints(questionSubmission.getCalculatedPoints());
        questionSubmissionDto.setAlteredGrade(questionSubmission.getAlteredGrade());
        List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList = new ArrayList<>();

        if (questionSubmissionComments) {
            for (QuestionSubmissionComment questionSubmissionComment : questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId())) {
                questionSubmissionCommentDtoList.add(questionSubmissionCommentService.toDto(questionSubmissionComment));
            }
        }

        questionSubmissionDto.setQuestionSubmissionCommentDtoList(questionSubmissionCommentDtoList);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();

        if (answerSubmissions) {
            List<AnswerMcSubmission> answerMcSubmissions = answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            List<AnswerEssaySubmission> answerEssaySubmissions = answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            List<AnswerFileSubmission> answerFileSubmissions = answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());

            for (AnswerMcSubmission answerMcSubmission : answerMcSubmissions) {
                answerSubmissionDtoList.add(answerSubmissionService.toDtoMC(answerMcSubmission));
            }

            for (AnswerEssaySubmission answerEssaySubmission : answerEssaySubmissions) {
                answerSubmissionDtoList.add(answerSubmissionService.toDtoEssay(answerEssaySubmission));
            }

            for (AnswerFileSubmission answerFileSubmission : answerFileSubmissions) {
                answerSubmissionDtoList.add(answerSubmissionService.toDtoFile(answerFileSubmission));
            }
        }

        questionSubmissionDto.setAnswerSubmissionDtoList(answerSubmissionDtoList);

        return questionSubmissionDto;
    }

    @Override
    public QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException {
        QuestionSubmission questionSubmission = new QuestionSubmission();
        questionSubmission.setQuestionSubmissionId(questionSubmissionDto.getQuestionSubmissionId());
        questionSubmission.setCalculatedPoints(questionSubmissionDto.getCalculatedPoints());
        questionSubmission.setAlteredGrade(questionSubmissionDto.getAlteredGrade());
        Optional<Submission> submission = submissionRepository.findById(questionSubmissionDto.getSubmissionId());

        if (submission.isEmpty()) {
            throw new DataServiceException("Submission with submissionID: " + questionSubmissionDto.getQuestionSubmissionId() + "  does not exist");
        }

        questionSubmission.setSubmission(submission.get());

        Optional<Question> question = questionRepository.findByAssessment_AssessmentIdAndQuestionId(submission.get().getAssessment().getAssessmentId(), questionSubmissionDto.getQuestionId());

        if (question.isEmpty()) {
            throw new DataServiceException("Question does not exist or does not belong to the submission and assessment");
        }

        questionSubmission.setQuestion(question.get());

        return questionSubmission;
    }

    private QuestionSubmission save(QuestionSubmission questionSubmission) {
        return questionSubmissionRepository.save(questionSubmission);
    }

    @Override
    public void deleteById(Long id) {
        questionSubmissionRepository.deleteByQuestionSubmissionId(id);
    }

    @Override
    @Transactional
    public QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission) {
        if (BooleanUtils.isTrue(answerMcSubmission.getAnswerMc().getCorrect())) {
            questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
        } else {
            questionSubmission.setCalculatedPoints(0f);
        }

        return questionSubmissionRepository.save(questionSubmission);
    }

    @Override
    public void validateDtoPost(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, Long submissionId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException {
        if (questionSubmissionDto.getQuestionId() == null) {
            throw new IdMissingException(TextConstants.ID_MISSING);
        }

        if (questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestion_QuestionId(assessmentId, submissionId, questionSubmissionDto.getQuestionId())) {
            throw new DuplicateQuestionException("Error 123: A question submission with question id " + questionSubmissionDto.getQuestionId() + " already exists in assessment with id " + assessmentId);
        }

        if (questionSubmissionDto.getAlteredGrade() != null && student) {
            throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId).toUri());

        return headers;
    }

    @Override
    public void validateAndPrepareQuestionSubmissionList(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws IdInPostException, DataServiceException, InvalidUserException, IdMissingException, DuplicateQuestionException, AnswerNotMatchingException, AnswerSubmissionNotMatchingException, ExceedingLimitException, TypeNotSupportedException {
        try {
            for (QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                if (questionSubmissionDto.getQuestionSubmissionId() != null) {
                    throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
                }

                validateDtoPost(questionSubmissionDto, assessmentId, submissionId, student);
                questionSubmissionDto.setSubmissionId(submissionId);
                QuestionSubmission questionSubmission = fromDto(questionSubmissionDto);

                if (questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.MC)
                    || questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.ESSAY)
                    || questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.FILE)) {
                    if (questionSubmissionDto.getAnswerSubmissionDtoList() != null) {
                        if (questionSubmissionDto.getAnswerSubmissionDtoList().size() > 1) {
                            throw new ExceedingLimitException("Error 145: Multiple choice and essay questions can only have one answer submission.");
                        } else if (CollectionUtils.isEmpty(questionSubmissionDto.getAnswerSubmissionDtoList())) {
                            questionSubmissionDto.getAnswerSubmissionDtoList().add(new AnswerSubmissionDto());
                        }
                    } else {
                        questionSubmissionDto.setAnswerSubmissionDtoList(new ArrayList<>());
                        questionSubmissionDto.getAnswerSubmissionDtoList().add(new AnswerSubmissionDto());
                    }

                }

                for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                    if (answerSubmissionDto.getAnswerId() != null) {
                        Optional<AnswerMc> answerMc = answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionSubmission.getQuestion().getQuestionId(), answerSubmissionDto.getAnswerId());

                        if (answerMc.isEmpty()) {
                            throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There is invalid data in the request. No question submissions or answer submissions will be created: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void validateQuestionSubmission(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException {
        try {
            QuestionSubmission questionSubmission = questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionDto.getQuestionSubmissionId());

            for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                if (answerSubmissionDto.getAnswerSubmissionId() == null) {
                    throw new IdMissingException("Error 125: An existing answer submission id must be included in the request.");
                }

                switch (questionSubmission.getQuestion().getQuestionType().toString()) {
                    case "MC":
                        Optional<AnswerMcSubmission> answerMcSubmission = answerMcSubmissionRepository.findById(answerSubmissionDto.getAnswerSubmissionId());

                        if (answerMcSubmission.isEmpty()) {
                            throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
                        }

                        if (answerSubmissionDto.getAnswerId() != null) {
                            Optional<AnswerMc> answerMc = answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionSubmission.getQuestion().getQuestionId(), answerSubmissionDto.getAnswerId());

                            if (answerMc.isEmpty()) {
                                throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
                            }
                        }

                        break;
                    case "ESSAY":
                        Optional<AnswerEssaySubmission> answerEssaySubmission = answerEssaySubmissionRepository.findById(answerSubmissionDto.getAnswerSubmissionId());

                        if (answerEssaySubmission.isEmpty()) {
                            throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
                        }

                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There is invalid data in the request. No question submissions or answer submissions will be updated: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void canSubmit(SecuredInfo securedInfo, long experimentId) throws ApiException, AssignmentAttemptException, IOException, TerracottaConnectorException {
        // There are two possible ways to do this check. First, and preferred, is using LTI custom variable substitution to get the allowed attempts
        // and the number of student attempts. The second is by making LMS API calls to get the same information.

        // (Approach #1) Using LTI custom variable substitution
        if (securedInfo.getAllowedAttempts() != null && securedInfo.getAllowedAttempts() == -1) {
            // unlimited attempts
            return;
        }

        if (securedInfo.getAllowedAttempts() != null && securedInfo.getStudentAttempts() != null) {
            if (securedInfo.getStudentAttempts() < securedInfo.getAllowedAttempts()) {
                // more attempts left
                return;
            }

            // attempts limit reached
            throw new AssignmentAttemptException(TextConstants.MAX_SUBMISSION_ATTEMPTS_REACHED);
        }

        // (Approach #2) Using LMS API calls
        Assignment assignment = assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, securedInfo.getLmsAssignmentId());
        LtiUserEntity instructorUser = assignment.getExposure().getExperiment().getCreatedBy();
        Optional<LmsAssignment> lmsAssignment = apiClient.listAssignment(instructorUser, securedInfo.getLmsCourseId(), securedInfo.getLmsAssignmentId());
        List<LmsSubmission> submissionsList = apiClient.listSubmissions(instructorUser, securedInfo.getLmsAssignmentId(), securedInfo.getLmsCourseId());

        Optional<LmsSubmission> submission = submissionsList.stream()
            .filter(sub -> sub.getUser() != null)
            .filter(sub -> sub.getUserId() == Integer.parseInt(securedInfo.getLmsUserId()))
            .findFirst();

        if (lmsAssignment.isEmpty() || submission.isEmpty()) {
            // no extends assignment and no submissions exist
            return;
        }

        int allowedAttempts = lmsAssignment.get().getAllowedAttempts();

        if (submission.get().getAttempt() == null || allowedAttempts <= 0) {
            // allowed attempts is infinite
            return;
        }

        Long attempt = submission.get().getAttempt();

        if (attempt.intValue() < allowedAttempts) {
            return;
        }

        // all allowable submission checks have failed, disallow submit attempt
        throw new AssignmentAttemptException(TextConstants.MAX_SUBMISSION_ATTEMPTS_REACHED);
    }

    public List<QuestionSubmissionDto> handleFileQuestionSubmission(MultipartFile file, String questionSubmissionDtoStr, long experimentId, long assessmentId, long submissionId, boolean student, SecuredInfo securedInfo)
            throws IOException, ApiException, AssignmentAttemptException, IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException,
                AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException, TerracottaConnectorException {
        String fileName = file.getResource().getFilename();
        File tempFile = getFile(file, file.getName());

        FileSubmissionLocal fileSubmissionLocal = fileStorageService.saveFileSubmissionLocal(file);
        QuestionSubmissionDto questionSubmissionDto = objectMapper.readValue(questionSubmissionDtoStr, QuestionSubmissionDto.class);
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setFileName(fileName);
        answerSubmissionDto.setMimeType(file.getContentType());
        answerSubmissionDto.setFile(tempFile);
        answerSubmissionDto.setFileUri(fileSubmissionLocal.getFilePath());

        if (fileSubmissionLocal.isCompressed()) {
            answerSubmissionDto.setEncryptionPhrase(fileSubmissionLocal.getEncryptionPhrase());
            answerSubmissionDto.setEncryptionMethod(fileSubmissionLocal.getEncryptionMethod());
        }

        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(answerSubmissionDto);
        questionSubmissionDto.setAnswerSubmissionDtoList(answerSubmissionDtoList);

        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();
        questionSubmissionDtoList.add(questionSubmissionDto);

        canSubmit(securedInfo, experimentId);
        validateAndPrepareQuestionSubmissionList(questionSubmissionDtoList, assessmentId, submissionId, student);

        return postQuestionSubmissions(questionSubmissionDtoList, assessmentId, submissionId, student);
    }

    public List<QuestionSubmissionDto> handleFileQuestionSubmissionUpdate(MultipartFile file, String questionSubmissionDtoStr, long experimentId, long assessmentId, long submissionId, long questionSubmissionId, boolean student, SecuredInfo securedInfo)
            throws IOException, ApiException, AssignmentAttemptException, IdInPostException, DataServiceException, DuplicateQuestionException, InvalidUserException, IdMissingException,
                AnswerSubmissionNotMatchingException, AnswerNotMatchingException, ExceedingLimitException, TypeNotSupportedException, QuestionSubmissionNotMatchingException, TerracottaConnectorException {
        QuestionSubmissionDto questionSubmissionDto = objectMapper.readValue(questionSubmissionDtoStr, QuestionSubmissionDto.class);
        QuestionSubmission questionSubmission = questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);

        if (questionSubmission == null) {
            throw new QuestionSubmissionNotMatchingException(TextConstants.QUESTION_SUBMISSION_NOT_MATCHING);
        }

        List<AnswerFileSubmission> answerFileSubmissions = answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);

        CollectionUtils.emptyIfNull(answerFileSubmissions).stream()
            .forEach(
                answerFileSubmission -> {
                    // remove file from file system
                    File existingFileSubmission = fileStorageService.getFileSubmissionLocal(answerFileSubmission.getAnswerFileSubmissionId());

                    try {
                        if (Files.deleteIfExists(existingFileSubmission.toPath())) {
                            log.info("File submission deleted: file name: '{}', answer submission ID: '{}'", answerFileSubmission.getFileName(), answerFileSubmission.getAnswerFileSubmissionId());
                        } else {
                            log.info("File submission NOT deleted: file name: '{}', answer submission ID: '{}'", answerFileSubmission.getFileName(), answerFileSubmission.getAnswerFileSubmissionId());
                        }
                    } catch (Exception e) {
                        log.error("File submission NOT deleted: file name: '{}', answer submission ID: '{}'", answerFileSubmission.getFileName(), answerFileSubmission.getAnswerFileSubmissionId(), e);
                    }

                    // remove row from database
                    answerFileSubmissionRepository.delete(answerFileSubmission);
                }
            );

        String fileName = file.getResource().getFilename();
        File tempFile = getFile(file, file.getName());

        FileSubmissionLocal fileSubmissionLocal = fileStorageService.saveFileSubmissionLocal(file);
        AnswerSubmissionDto answerSubmissionDto = new AnswerSubmissionDto();
        answerSubmissionDto.setFileName(fileName);
        answerSubmissionDto.setMimeType(file.getContentType());
        answerSubmissionDto.setFile(tempFile);
        answerSubmissionDto.setFileUri(fileSubmissionLocal.getFilePath());

        if (fileSubmissionLocal.isCompressed()) {
            answerSubmissionDto.setEncryptionPhrase(fileSubmissionLocal.getEncryptionPhrase());
            answerSubmissionDto.setEncryptionMethod(fileSubmissionLocal.getEncryptionMethod());
        }

        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        answerSubmissionDtoList.add(answerSubmissionDto);
        questionSubmissionDto.setAnswerSubmissionDtoList(answerSubmissionDtoList);

        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();
        questionSubmissionDtoList.add(questionSubmissionDto);

        canSubmit(securedInfo, experimentId);
        validateAndPrepareQuestionSubmissionList(questionSubmissionDtoList, assessmentId, submissionId, student);

        updateQuestionSubmissions(Collections.singletonMap(questionSubmission, questionSubmissionDto), student);

        return questionSubmissionDtoList;
    }

    private File getFile(MultipartFile multipartFile, String fileName) {
        File tempFile = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        } catch (IOException e) {
            log.error("Error while converting Multipart file to file {}", tempFile.getName());
        }

        return tempFile;
    }

}
