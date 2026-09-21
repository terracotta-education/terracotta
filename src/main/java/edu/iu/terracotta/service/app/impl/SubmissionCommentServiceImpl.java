package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class SubmissionCommentServiceImpl implements SubmissionCommentService {

    private final LtiUserRepository ltiUserRepository;
    private final SubmissionCommentRepository submissionCommentRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public List<SubmissionCommentDto> getSubmissionComments(Long submissionId) {
        return CollectionUtils.emptyIfNull(submissionCommentRepository.findBySubmission_SubmissionId(submissionId)).stream()
            .map(submissionComment -> toDto(submissionComment))
            .toList();
    }

    @Override
    public SubmissionCommentDto postSubmissionComment(SubmissionCommentDto submissionCommentDto, long submissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException {
        if (submissionCommentDto.getSubmissionCommentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        submissionCommentDto.setSubmissionId(submissionRepository.findById(submissionId).map(Submission::getUuid).orElse(null));
        LtiUserEntity user = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        submissionCommentDto.setCreator(user.getDisplayName());
        SubmissionComment submissionComment;

        try {
            submissionComment = fromDto(submissionCommentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create submission comment: " + ex.getMessage(), ex);
        }

        return toDto(submissionCommentRepository.save(submissionComment));
    }

    @Override
    public void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto) {
        submissionComment.setComment(submissionCommentDto.getComment());
        submissionCommentRepository.saveAndFlush(submissionComment);
    }

    @Override
    public SubmissionComment getSubmissionComment(Long id) {
        return submissionCommentRepository.findBySubmissionCommentId(id);
    }

    @Override
    public SubmissionComment getSubmissionCommentByUuid(UUID uuid) throws SubmissionCommentNotMatchingException {
        return Optional.ofNullable(submissionCommentRepository.findByUuid(uuid))
            .orElseThrow(() -> new SubmissionCommentNotMatchingException(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING));
    }

    @Override
    public long getSubmissionCommentIdByUuid(UUID uuid) throws SubmissionCommentNotMatchingException {
        return submissionCommentRepository.findIdByUuid(uuid)
            .orElseThrow(() -> new SubmissionCommentNotMatchingException(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING));
    }

    @Override
    public SubmissionCommentDto toDto(SubmissionComment submissionComment) {
        SubmissionCommentDto submissionCommentDto = new SubmissionCommentDto();
        submissionCommentDto.setSubmissionCommentId(submissionComment.getUuid());
        submissionCommentDto.setSubmissionId(submissionComment.getSubmission().getUuid());
        submissionCommentDto.setComment(submissionComment.getComment());
        submissionCommentDto.setCreator(submissionComment.getCreator());

        return submissionCommentDto;
    }

    @Override
    public SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException {
        Optional<Submission> submission = Optional.ofNullable(submissionRepository.findByUuid(submissionCommentDto.getSubmissionId()));

        if (submission.isEmpty()) {
            throw new DataServiceException("The submission for the submission comment doesn't exist.");
        }

        SubmissionComment submissionComment = new SubmissionComment();

        // submissionCommentDto.getSubmissionCommentId() (now a uuid) is intentionally not set on a new
        // SubmissionComment here - the real numeric id/uuid are both IDENTITY/@PrePersist generated at
        // insert time regardless.
        submissionComment.setComment(submissionCommentDto.getComment());
        submissionComment.setCreator(submissionCommentDto.getCreator());

        submissionComment.setSubmission(submission.get());

        return submissionComment;
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        submissionCommentRepository.deleteBySubmissionCommentId(id);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, UUID experimentId, UUID conditionId, UUID treatmentId, UUID assessmentId, UUID submissionId, UUID submissionCommentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/submission_comments/{submissionCommentId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, submissionCommentId).toUri());
        return headers;
    }

}
