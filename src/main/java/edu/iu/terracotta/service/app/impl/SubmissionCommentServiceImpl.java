package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Component
public class SubmissionCommentServiceImpl implements SubmissionCommentService {

    @Autowired private AllRepositories allRepositories;

    @Override
    public List<SubmissionCommentDto> getSubmissionComments(Long submissionId) {
        return CollectionUtils.emptyIfNull(allRepositories.submissionCommentRepository.findBySubmission_SubmissionId(submissionId)).stream()
            .map(submissionComment -> toDto(submissionComment))
            .toList();
    }

    @Override
    public SubmissionCommentDto postSubmissionComment(SubmissionCommentDto submissionCommentDto, long submissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException {
        if (submissionCommentDto.getSubmissionCommentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        submissionCommentDto.setSubmissionId(submissionId);
        LtiUserEntity user = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        submissionCommentDto.setCreator(user.getDisplayName());
        SubmissionComment submissionComment;

        try {
            submissionComment = fromDto(submissionCommentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create submission comment: " + ex.getMessage(), ex);
        }

        return toDto(allRepositories.submissionCommentRepository.save(submissionComment));
    }

    @Override
    public void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto) {
        submissionComment.setComment(submissionCommentDto.getComment());
        allRepositories.submissionCommentRepository.saveAndFlush(submissionComment);
    }

    @Override
    public SubmissionComment getSubmissionComment(Long id) {
        return allRepositories.submissionCommentRepository.findBySubmissionCommentId(id);
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
        Optional<Submission> submission = allRepositories.submissionRepository.findById(submissionCommentDto.getSubmissionId());

        if (!submission.isPresent()) {
            throw new DataServiceException("The submission for the submission comment doesn't exist.");
        }

        SubmissionComment submissionComment = new SubmissionComment();
        submissionComment.setSubmissionCommentId(submissionCommentDto.getSubmissionCommentId());
        submissionComment.setComment(submissionCommentDto.getComment());
        submissionComment.setCreator(submissionCommentDto.getCreator());

        submissionComment.setSubmission(submission.get());

        return submissionComment;
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.submissionCommentRepository.deleteBySubmissionCommentId(id);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId, long submissionCommentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/submission_comments/{submissionCommentId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, submissionCommentId).toUri());
        return headers;
    }

}
