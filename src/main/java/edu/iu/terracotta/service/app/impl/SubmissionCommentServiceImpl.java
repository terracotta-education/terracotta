package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SubmissionCommentServiceImpl implements SubmissionCommentService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<SubmissionComment> findAllBySubmissionId(Long submissionId) {
        return allRepositories.submissionCommentRepository.findBySubmission_SubmissionId(submissionId);
    }

    @Override
    public SubmissionCommentDto toDto(SubmissionComment submissionComment) {

        SubmissionCommentDto submissionCommentDto = new SubmissionCommentDto();
        submissionCommentDto.setSubmissionCommentId(submissionComment.getSubmissionCommentId());
        submissionCommentDto.setSubmissionId(submissionComment.getSubmission().getSubmissionId());
        submissionCommentDto.setComment(submissionComment.getComment());
        submissionCommentDto.setCreator(submissionComment.getCreator());

        return submissionCommentDto;
    }

    @Override
    public SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException {

        SubmissionComment submissionComment = new SubmissionComment();
        submissionComment.setSubmissionCommentId(submissionCommentDto.getSubmissionCommentId());
        submissionComment.setComment(submissionCommentDto.getComment());
        submissionComment.setCreator(submissionCommentDto.getCreator());
        Optional<Submission> submission = allRepositories.submissionRepository.findById(submissionCommentDto.getSubmissionId());
        if(submission.isPresent()){
            submissionComment.setSubmission(submission.get());
        } else {
            throw new DataServiceException("The submission for the submission comment doesn't exist.");
        }

        return submissionComment;
    }

    @Override
    public SubmissionComment save(SubmissionComment submissionComment) { return allRepositories.submissionCommentRepository.save(submissionComment); }

    @Override
    public Optional<SubmissionComment> findById(Long id) { return allRepositories.submissionCommentRepository.findById(id); }

    @Override
    public LtiUserEntity findByUserKey(String key) { return allRepositories.ltiUserRepository.findByUserKey(key); }

    @Override
    public void saveAndFlush(SubmissionComment submissionCommentToChange) { allRepositories.submissionCommentRepository.saveAndFlush(submissionCommentToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.submissionCommentRepository.deleteById(id); }

    @Override
    public boolean submissionCommentBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long submissionCommentId) {
        return allRepositories.submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(
                assessmentId, submissionId, submissionCommentId);
    }
}
