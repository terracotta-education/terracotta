package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class QuestionSubmissionCommentServiceImpl implements QuestionSubmissionCommentService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<QuestionSubmissionComment> findAllByQuestionSubmissionId(Long questionSubmissionId) {
        return allRepositories.questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId);
    }

    @Override
    public List<QuestionSubmissionCommentDto> getQuestionSubmissionComments(Long questionSubmissionId){
        List<QuestionSubmissionComment> questionSubmissionComments= findAllByQuestionSubmissionId(questionSubmissionId);
        List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList = new ArrayList<>();
        for(QuestionSubmissionComment questionSubmissionComment : questionSubmissionComments){
            questionSubmissionCommentDtoList.add(toDto(questionSubmissionComment));
        }
        return questionSubmissionCommentDtoList;
    }

    @Override
    public QuestionSubmissionComment getQuestionSubmissionComment(Long id){
        return allRepositories.questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(id);
    }

    @Override
    public QuestionSubmissionCommentDto postQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionId, String userId) throws IdInPostException, DataServiceException {
        if(questionSubmissionCommentDto.getQuestionSubmissionCommentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionId);
        LtiUserEntity user = findByUserKey(userId);
        questionSubmissionCommentDto.setCreator(user.getDisplayname());
        QuestionSubmissionComment questionSubmissionComment;
        try {
            questionSubmissionComment = fromDto(questionSubmissionCommentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create question submission comment: " + ex.getMessage());
        }
        return toDto(save(questionSubmissionComment));
    }

    @Override
    public void updateQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionCommentId, long experimentId, long submissionId, String userId) throws DataServiceException {
        QuestionSubmissionComment questionSubmissionComment = getQuestionSubmissionComment(questionSubmissionCommentId);
        LtiUserEntity user = findByUserKey(userId);
        if(!user.getDisplayname().equals(questionSubmissionComment.getCreator())){
            throw new DataServiceException("Error 122: Only the creator of a comment can edit their own comment.");
        }
        questionSubmissionComment.setComment(questionSubmissionCommentDto.getComment());
        saveAndFlush(questionSubmissionComment);
    }

    @Override
    public QuestionSubmissionCommentDto toDto(QuestionSubmissionComment questionSubmissionComment) {

        QuestionSubmissionCommentDto questionSubmissionCommentDto = new QuestionSubmissionCommentDto();
        questionSubmissionCommentDto.setQuestionSubmissionCommentId(questionSubmissionComment.getQuestionSubmissionCommentId());
        questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionComment.getQuestionSubmission().getQuestionSubmissionId());
        questionSubmissionCommentDto.setComment(questionSubmissionComment.getComment());
        questionSubmissionCommentDto.setCreator(questionSubmissionComment.getCreator());

        return questionSubmissionCommentDto;
    }

    @Override
    public QuestionSubmissionComment fromDto(QuestionSubmissionCommentDto questionSubmissionCommentDto) throws DataServiceException {

        QuestionSubmissionComment questionSubmissionComment = new QuestionSubmissionComment();
        questionSubmissionComment.setQuestionSubmissionCommentId(questionSubmissionCommentDto.getQuestionSubmissionCommentId());
        questionSubmissionComment.setComment(questionSubmissionCommentDto.getComment());
        questionSubmissionComment.setCreator(questionSubmissionCommentDto.getCreator());
        Optional<QuestionSubmission> questionSubmission = allRepositories.questionSubmissionRepository.findById(questionSubmissionCommentDto.getQuestionSubmissionId());
        if(questionSubmission.isPresent()){
            questionSubmissionComment.setQuestionSubmission(questionSubmission.get());
        } else {
            throw new DataServiceException("The question submission for the question submission comment doesn't exist.");
        }

        return questionSubmissionComment;
    }

    @Override
    public QuestionSubmissionComment save(QuestionSubmissionComment questionSubmissionComment) {
        return allRepositories.questionSubmissionCommentRepository.save(questionSubmissionComment);
    }

    @Override
    public Optional<QuestionSubmissionComment> findById(Long id) { return allRepositories.questionSubmissionCommentRepository.findById(id); }

    @Override
    public LtiUserEntity findByUserKey(String key) { return allRepositories.ltiUserRepository.findByUserKey(key); }

    @Override
    public void saveAndFlush(QuestionSubmissionComment questionSubmissionCommentToChange) {
        allRepositories.questionSubmissionCommentRepository.saveAndFlush(questionSubmissionCommentToChange);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.questionSubmissionCommentRepository.deleteByQuestionSubmissionCommentId(id); }

    @Override
    public boolean questionSubmissionCommentBelongsToQuestionSubmission(Long questionSubmissionId, Long questionSubmissionCommentId){
        return allRepositories.questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(
                questionSubmissionId, questionSubmissionCommentId);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long questionSubmissionCommentId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments/{question_submission_comment_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, questionSubmissionCommentId).toUri());
        return headers;
    }
}
