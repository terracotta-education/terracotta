package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

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
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.questionSubmissionCommentRepository.deleteById(id); }

    @Override
    public boolean questionSubmissionCommentBelongsToQuestionSubmission(Long questionSubmissionId, Long questionSubmissionCommentId){
        return allRepositories.questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(
                questionSubmissionId, questionSubmissionCommentId);
    }
}
