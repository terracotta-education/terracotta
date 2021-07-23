package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QuestionSubmissionServiceImpl implements QuestionSubmissionService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    QuestionSubmissionCommentService questionSubmissionCommentService;

    @Override
    public List<QuestionSubmission> findAllBySubmissionId(Long submissionId) {
        return allRepositories.questionSubmissionRepository.findBySubmission_SubmissionId(submissionId);
    }

    @Override
    public List<QuestionSubmissionDto> getQuestionSubmissions(Long submissionId){
        List<QuestionSubmission> questionSubmissions = findAllBySubmissionId(submissionId);
        List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();
        for(QuestionSubmission questionSubmission : questionSubmissions){
            questionSubmissionDtoList.add(toDto(questionSubmission, false));
        }
        return questionSubmissionDtoList;
    }

    @Override
    public QuestionSubmission getQuestionSubmission(Long id) {
        return allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(id);
    }

    @Override
    @Transactional
    public void updateQuestionSubmissions(Map<QuestionSubmission, QuestionSubmissionDto> map, boolean student) throws InvalidUserException{
        for(Map.Entry<QuestionSubmission, QuestionSubmissionDto> entry : map.entrySet()){
            QuestionSubmission questionSubmission = entry.getKey();
            QuestionSubmissionDto questionSubmissionDto = entry.getValue();

            if(questionSubmissionDto.getAlteredGrade() != null && student){
                throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
            }
            questionSubmission.setAlteredGrade(questionSubmissionDto.getAlteredGrade());
            save(questionSubmission);
        }
    }

    @Override
    public QuestionSubmissionDto toDto(QuestionSubmission questionSubmission, boolean questionSubmissionComments) {

        QuestionSubmissionDto questionSubmissionDto = new QuestionSubmissionDto();
        questionSubmissionDto.setQuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
        questionSubmissionDto.setSubmissionId(questionSubmission.getSubmission().getSubmissionId());
        questionSubmissionDto.setQuestionId(questionSubmission.getQuestion().getQuestionId());
        questionSubmissionDto.setCalculatedPoints(questionSubmission.getCalculatedPoints());
        questionSubmissionDto.setAlteredGrade(questionSubmission.getAlteredGrade());
        List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList = new ArrayList<>();
        if(questionSubmissionComments){
            List<QuestionSubmissionComment> questionSubmissionCommentList =
                    allRepositories.questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
            for(QuestionSubmissionComment questionSubmissionComment : questionSubmissionCommentList) {
                questionSubmissionCommentDtoList.add(questionSubmissionCommentService.toDto(questionSubmissionComment));
            }
        }
        questionSubmissionDto.setQuestionSubmissionCommentDtoList(questionSubmissionCommentDtoList);
        return questionSubmissionDto;
    }

    @Override
    public QuestionSubmission fromDto(QuestionSubmissionDto questionSubmissionDto) throws DataServiceException {

        QuestionSubmission questionSubmission = new QuestionSubmission();
        questionSubmission.setQuestionSubmissionId(questionSubmissionDto.getQuestionSubmissionId());
        questionSubmission.setCalculatedPoints(questionSubmissionDto.getCalculatedPoints());
        questionSubmission.setAlteredGrade(questionSubmissionDto.getAlteredGrade());
        Optional<Submission> submission = allRepositories.submissionRepository.findById(questionSubmissionDto.getSubmissionId());
        if(submission.isPresent()) {
            questionSubmission.setSubmission(submission.get());
        } else {
            throw new DataServiceException("Submission with submissionID: " + questionSubmissionDto.getQuestionSubmissionId() +  "  does not exist");
        }
        Optional<Question> question = allRepositories.questionRepository.findByAssessment_AssessmentIdAndQuestionId(submission.get().getAssessment().getAssessmentId(), questionSubmissionDto.getQuestionId());
        if(question.isPresent()) {
            questionSubmission.setQuestion(question.get());
        } else {
            throw new DataServiceException("Question does not exist or does not belong to the submission assessment");
        }

        return questionSubmission;
    }

    @Override
    public QuestionSubmission save(QuestionSubmission questionSubmission) { return allRepositories.questionSubmissionRepository.save(questionSubmission); }

    @Override
    public Optional<QuestionSubmission> findById(Long id) { return allRepositories.questionSubmissionRepository.findById(id); }

    @Override
    public boolean existsByAssessmentIdAndQuestionId(Long assessmentId, Long questionId) {
        return allRepositories.questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndQuestion_QuestionId(assessmentId, questionId);
    }

    @Override
    public void saveAndFlush(QuestionSubmission questionSubmissionToChange) { allRepositories.questionSubmissionRepository.saveAndFlush(questionSubmissionToChange); }

    @Override
    public void deleteById(Long id) { allRepositories.questionSubmissionRepository.deleteByQuestionSubmissionId(id); }

    @Override
    public boolean questionSubmissionBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long questionSubmissionId) {
        return allRepositories.questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(
                assessmentId, submissionId, questionSubmissionId);
    }

    @Override
    @Transactional
    public QuestionSubmission automaticGradingMC(QuestionSubmission questionSubmission, AnswerMcSubmission answerMcSubmission){
        if(answerMcSubmission.getAnswerMc().getCorrect()){
            questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
        } else {
            questionSubmission.setCalculatedPoints(Float.valueOf("0"));
        }
        allRepositories.questionSubmissionRepository.save(questionSubmission);
        return questionSubmission;
    }

    @Override
    public void validateDto(QuestionSubmissionDto questionSubmissionDto, Long assessmentId, boolean student) throws IdMissingException, DuplicateQuestionException, InvalidUserException {
        if(questionSubmissionDto.getQuestionId() == null){
            throw new IdMissingException(TextConstants.ID_MISSING);
        }
        if(existsByAssessmentIdAndQuestionId(assessmentId, questionSubmissionDto.getQuestionId())){
            throw new DuplicateQuestionException("Error 123: A question submission with question id " + questionSubmissionDto.getQuestionId() + " already exists in assessment with id " + assessmentId);
        }
        if(questionSubmissionDto.getAlteredGrade() != null && student){
            throw new InvalidUserException(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId).toUri());
        return headers;
    }
}