package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ToolDeployment;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QuestionSubmissionServiceImpl implements QuestionSubmissionService {

    static final Logger log = LoggerFactory.getLogger(QuestionSubmissionServiceImpl.class);

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    private AnswerService answerService;

    @Autowired
    AnswerSubmissionService answerSubmissionService;

    @Autowired
    QuestionSubmissionCommentService questionSubmissionCommentService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Override
    public List<QuestionSubmission> findAllBySubmissionId(Long submissionId) {
        return allRepositories.questionSubmissionRepository.findBySubmission_SubmissionId(submissionId);
    }

    @Override
    public List<QuestionSubmissionDto> getQuestionSubmissions(long submissionId, boolean answerSubmissions, boolean questionSubmissionComments, long assessmentId, boolean isStudent) throws AssessmentNotMatchingException {
        boolean showCorrectAnswers = true;

        if (isStudent) {
            Optional<Assessment> assessment = allRepositories.assessmentRepository.findById(assessmentId);

            if (!assessment.isPresent()) {
                throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
            }

            answerSubmissions = assessment.get().canViewResponses();
            questionSubmissionComments = answerSubmissions;
            showCorrectAnswers = assessment.get().canViewCorrectAnswers();
        }

        List<QuestionSubmission> questionSubmissions = findAllBySubmissionId(submissionId);
        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();

        for (QuestionSubmission questionSubmission : questionSubmissions) {
            questionSubmissionDtoList.add(toDto(questionSubmission, answerSubmissions, questionSubmissionComments, showCorrectAnswers));
        }

        return questionSubmissionDtoList;
    }

    @Override
    public QuestionSubmission getQuestionSubmission(Long id) {
        return allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(id);
    }

    @Override
    @Transactional
    //TODO this method isn't technically fully transactional. The dto is validated beforehand.
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
                if (questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.MC)) {
                    answerSubmissionService.updateAnswerMcSubmission(answerSubmissionDto.getAnswerSubmissionId(), answerSubmissionDto);
                } else if (questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.ESSAY)) {
                    answerSubmissionService.updateAnswerEssaySubmission(answerSubmissionDto.getAnswerSubmissionId(), answerSubmissionDto);
                }
            }
        }
    }

    @Override
    @Transactional
    //TODO this method isn't technically fully transactional. The dto is validated beforehand.
    public List<QuestionSubmissionDto> postQuestionSubmissions(List<QuestionSubmissionDto> questionSubmissionDtoList, long assessmentId, long submissionId, boolean student) throws DataServiceException {
        List<QuestionSubmissionDto> returnedDtoList = new ArrayList<>();
        try {
            for (QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                log.debug("Creating question submission: {}", questionSubmissionDto);
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

    private QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments, boolean showCorrectAnswers) {
        QuestionSubmissionDto questionSubmissionDto = toDto(questionSubmission, answerSubmissions, questionSubmissionComments);

        List<AnswerDto> answerDtos = answerService.findAllByQuestionIdMC(questionSubmission.getQuestion().getQuestionId(), showCorrectAnswers);
        questionSubmissionDto.setAnswerDtoList(answerDtos);

        return questionSubmissionDto;
    }

    @Override
    public QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean answerSubmissions, boolean questionSubmissionComments) {
        QuestionSubmissionDto questionSubmissionDto = new QuestionSubmissionDto();
        questionSubmissionDto.setQuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
        questionSubmissionDto.setSubmissionId(questionSubmission.getSubmission().getSubmissionId());
        questionSubmissionDto.setQuestionId(questionSubmission.getQuestion().getQuestionId());
        questionSubmissionDto.setCalculatedPoints(questionSubmission.getCalculatedPoints());
        questionSubmissionDto.setAlteredGrade(questionSubmission.getAlteredGrade());
        List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList = new ArrayList<>();
        if (questionSubmissionComments) {
            List<QuestionSubmissionComment> questionSubmissionCommentList =
                    allRepositories.questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            for (QuestionSubmissionComment questionSubmissionComment : questionSubmissionCommentList) {
                questionSubmissionCommentDtoList.add(questionSubmissionCommentService.toDto(questionSubmissionComment));
            }
        }
        questionSubmissionDto.setQuestionSubmissionCommentDtoList(questionSubmissionCommentDtoList);
        List<AnswerSubmissionDto> answerSubmissionDtoList = new ArrayList<>();
        if (answerSubmissions) {
            List<AnswerMcSubmission> answerMcSubmissions = allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            List<AnswerEssaySubmission> answerEssaySubmissions = allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            for (AnswerMcSubmission answerMcSubmission : answerMcSubmissions) {
                answerSubmissionDtoList.add(answerSubmissionService.toDtoMC(answerMcSubmission));
            }
            for (AnswerEssaySubmission answerEssaySubmission : answerEssaySubmissions) {
                answerSubmissionDtoList.add(answerSubmissionService.toDtoEssay(answerEssaySubmission));
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
        Optional<Submission> submission = allRepositories.submissionRepository.findById(questionSubmissionDto.getSubmissionId());
        if (submission.isPresent()) {
            questionSubmission.setSubmission(submission.get());
        } else {
            throw new DataServiceException("Submission with submissionID: " + questionSubmissionDto.getQuestionSubmissionId() + "  does not exist");
        }
        Optional<Question> question = allRepositories.questionRepository.findByAssessment_AssessmentIdAndQuestionId(submission.get().getAssessment().getAssessmentId(), questionSubmissionDto.getQuestionId());
        if (question.isPresent()) {
            questionSubmission.setQuestion(question.get());
        } else {
            throw new DataServiceException("Question does not exist or does not belong to the submission and assessment");
        }

        return questionSubmission;
    }

    @Override
    public QuestionSubmission save(QuestionSubmission questionSubmission) {
        return allRepositories.questionSubmissionRepository.save(questionSubmission);
    }

    @Override
    public Optional<QuestionSubmission> findById(Long id) {
        return allRepositories.questionSubmissionRepository.findById(id);
    }

    @Override
    public boolean existsByAssessmentIdAndSubmissionIdAndQuestionId(Long assessmentId, Long submissionId, Long questionId) {
        return allRepositories.questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestion_QuestionId(assessmentId, submissionId, questionId);
    }

    @Override
    public void saveAndFlush(QuestionSubmission questionSubmissionToChange) {
        allRepositories.questionSubmissionRepository.saveAndFlush(questionSubmissionToChange);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.questionSubmissionRepository.deleteByQuestionSubmissionId(id);
    }

    @Override
    public boolean questionSubmissionBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long questionSubmissionId) {
        return allRepositories.questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(
                assessmentId, submissionId, questionSubmissionId);
    }

    @Override
    @Transactional
    public QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission) {
        if (answerMcSubmission.getAnswerMc().getCorrect()) {
            questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
        } else {
            questionSubmission.setCalculatedPoints(Float.valueOf("0"));
        }
        allRepositories.questionSubmissionRepository.save(questionSubmission);
        return questionSubmission;
    }

    @Override
    public void validateDtoPost(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, Long submissionId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException {
        if (questionSubmissionDto.getQuestionId() == null) {
            throw new IdMissingException(TextConstants.ID_MISSING);
        }
        if (existsByAssessmentIdAndSubmissionIdAndQuestionId(assessmentId, submissionId, questionSubmissionDto.getQuestionId())) {
            throw new DuplicateQuestionException("Error 123: A question submission with question id " + questionSubmissionDto.getQuestionId() + " already exists in assessment with id " + assessmentId);
        }
        if (questionSubmissionDto.getAlteredGrade() != null && student) {
            throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions")
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

                handleAnswerSubmissionDtos(questionSubmission, questionSubmissionDto);

                for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                    if (answerSubmissionDto.getAnswerId() != null) {
                        Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionSubmission.getQuestion().getQuestionId(), answerSubmissionDto.getAnswerId());
                        if (!answerMc.isPresent()) {
                            throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There is invalid data in the request. No question submissions or answer submissions will be created: " + ex.getMessage(), ex);
        }
    }

    private void handleAnswerSubmissionDtos(QuestionSubmission questionSubmission, QuestionSubmissionDto questionSubmissionDto) throws ExceedingLimitException {
        if (!questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.MC) && !questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.ESSAY)) {
            return;
        }

        if (questionSubmissionDto.getAnswerSubmissionDtoList() != null) {
            if (questionSubmissionDto.getAnswerSubmissionDtoList().size() > 1) {
                throw new ExceedingLimitException("Error 145: Multiple choice and essay questions can only have one answer submission.");
            } else if (questionSubmissionDto.getAnswerSubmissionDtoList().isEmpty()) {
                questionSubmissionDto.getAnswerSubmissionDtoList().add(new AnswerSubmissionDto());
            }
        } else {
            questionSubmissionDto.setAnswerSubmissionDtoList(new ArrayList<>());
            questionSubmissionDto.getAnswerSubmissionDtoList().add(new AnswerSubmissionDto());
        }
    }

    @Override
    public void validateQuestionSubmission(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException {
        try {
            QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionDto.getQuestionSubmissionId());
            for (AnswerSubmissionDto answerSubmissionDto : questionSubmissionDto.getAnswerSubmissionDtoList()) {
                if (answerSubmissionDto.getAnswerSubmissionId() == null) {
                    throw new IdMissingException("Error 125: An existing answer submission id must be included in the request.");
                }

                switch (questionSubmission.getQuestion().getQuestionType()) {
                    case MC:
                        validateQuestionSubmissionMC(answerSubmissionDto, questionSubmission);
                        break;
                    case ESSAY:
                        validateQuestionSubmissionEssay(answerSubmissionDto);
                        break;
                    default:
                        throw new DataServiceException(TextConstants.ANSWER_TYPE_NOT_SUPPORTED);
                }
            }
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There is invalid data in the request. No question submissions or answer submissions will be updated: " + ex.getMessage(), ex);
        }
    }

    private void validateQuestionSubmissionMC(AnswerSubmissionDto answerSubmissionDto, QuestionSubmission questionSubmission) throws AnswerSubmissionNotMatchingException, AnswerNotMatchingException {
        Optional<AnswerMcSubmission> answerMcSubmission = allRepositories.answerMcSubmissionRepository.findById(answerSubmissionDto.getAnswerSubmissionId());
        if (!answerMcSubmission.isPresent()) {
            throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
        }

        if (answerSubmissionDto.getAnswerId() != null) {
            Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findByQuestion_QuestionIdAndAnswerMcId(questionSubmission.getQuestion().getQuestionId(), answerSubmissionDto.getAnswerId());

            if (!answerMc.isPresent()) {
                throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
            }
        }
    }

    private void validateQuestionSubmissionEssay(AnswerSubmissionDto answerSubmissionDto) throws AnswerSubmissionNotMatchingException {
        Optional<AnswerEssaySubmission> answerEssaySubmission = allRepositories.answerEssaySubmissionRepository.findById(answerSubmissionDto.getAnswerSubmissionId());

        if (!answerEssaySubmission.isPresent()) {
            throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void canSubmit(String canvasCourseId, String assignmentId, String canvasUserId, long deploymentId) throws CanvasApiException, AssignmentAttemptException, IOException {
        // We find the right deployment:
        Optional<ToolDeployment> toolDeployment = allRepositories.toolDeploymentRepository.findById(deploymentId);

        if (!toolDeployment.isPresent()) {
            return;
        }

        String litDeploymentId = toolDeployment.get().getLtiDeploymentId();
        List<PlatformDeployment> platformDeploymentList = allRepositories.platformDeploymentRepository.findByToolDeployments_LtiDeploymentId(litDeploymentId);

        if (platformDeploymentList.isEmpty()) {
            return;
        }

        PlatformDeployment platformDeployment = platformDeploymentList.get(0);

        Optional<AssignmentExtended> assignmentExtended = canvasAPIClient.listAssignment(canvasCourseId, Integer.parseInt(assignmentId), platformDeployment);
        List<edu.ksu.canvas.model.assignment.Submission> submissionsList = canvasAPIClient.listSubmissions(Integer.parseInt(assignmentId), canvasCourseId, platformDeployment);

        Optional<edu.ksu.canvas.model.assignment.Submission> submission = submissionsList.stream()
            .filter(sub -> { return sub.getUser().getId() == Integer.parseInt(canvasUserId); })
            .findFirst();

        if (!assignmentExtended.isPresent() || !submission.isPresent()) {
            // no extends assignment and no submissions exist
            return;
        }

        int allowedAttempts = assignmentExtended.get().getAllowedAttempts();

        if (submission.get().getAttempt() == null || allowedAttempts <= 0) {
            // allowed attempts is infinite
            return;
        }

        int attempt = submission.get().getAttempt();

        if (attempt < allowedAttempts) {
            return;
        }

        // all allowable submission checks have failed, disallow submit attempt
        throw new AssignmentAttemptException(TextConstants.MAX_SUBMISSION_ATTEMPTS_REACHED);
    }

}
