package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.SubmissionCommentService;
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
public class SubmissionCommentServiceImpl implements SubmissionCommentService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<SubmissionComment> findAllBySubmissionId(Long submissionId) {
        return allRepositories.submissionCommentRepository.findBySubmission_SubmissionId(submissionId);
    }

    @Override
    public List<SubmissionCommentDto> getSubmissionComments(Long submissionId){
        List<SubmissionComment> submissionComments = findAllBySubmissionId(submissionId);
        List<SubmissionCommentDto> submissionCommentDtoList = new ArrayList<>();
        for(SubmissionComment submissionComment : submissionComments){
            submissionCommentDtoList.add(toDto(submissionComment));
        }
        return submissionCommentDtoList;
    }

    @Override
    public SubmissionCommentDto postSubmissionComment(SubmissionCommentDto submissionCommentDto, long submissionId, String userId) throws IdInPostException, DataServiceException {
        if(submissionCommentDto.getSubmissionCommentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        submissionCommentDto.setSubmissionId(submissionId);
        LtiUserEntity user = findByUserKey(userId);
        submissionCommentDto.setCreator(user.getDisplayname());
        SubmissionComment submissionComment;
        try {
            submissionComment = fromDto(submissionCommentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create submission comment: " + ex.getMessage());
        }
        return toDto(save(submissionComment));
    }

    @Override
    public void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto){
        submissionComment.setComment(submissionCommentDto.getComment());
        saveAndFlush(submissionComment);
    }

    @Override
    public SubmissionComment getSubmissionComment(Long id){ return allRepositories.submissionCommentRepository.findBySubmissionCommentId(id); }

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
    public Optional<SubmissionComment> findBySubmissionIdAndSubmissionCommentId(Long submissionId, Long submissionCommentId) {
        return allRepositories.submissionCommentRepository.findBySubmission_SubmissionIdAndSubmissionCommentId(submissionId, submissionCommentId);
    }

    @Override
    public LtiUserEntity findByUserKey(String key) { return allRepositories.ltiUserRepository.findByUserKey(key); }

    @Override
    public void saveAndFlush(SubmissionComment submissionCommentToChange) { allRepositories.submissionCommentRepository.saveAndFlush(submissionCommentToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.submissionCommentRepository.deleteBySubmissionCommentId(id); }

    @Override
    public boolean submissionCommentBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long submissionCommentId) {
        return allRepositories.submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(
                assessmentId, submissionId, submissionCommentId);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId, long submissionCommentId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments/{submission_comment_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, submissionCommentId).toUri());
        return headers;
    }
}
