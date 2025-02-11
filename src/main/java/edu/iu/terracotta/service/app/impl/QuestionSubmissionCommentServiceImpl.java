package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.dao.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
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
public class QuestionSubmissionCommentServiceImpl implements QuestionSubmissionCommentService {

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;

    @Override
    public List<QuestionSubmissionCommentDto> getQuestionSubmissionComments(Long questionSubmissionId) {
        return CollectionUtils.emptyIfNull(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmissionId)).stream()
            .map(questionSubmissionComment -> toDto(questionSubmissionComment))
            .toList();
    }

    @Override
    public QuestionSubmissionComment getQuestionSubmissionComment(Long id) {
        return questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(id);
    }

    @Override
    public QuestionSubmissionCommentDto postQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException {
        if (questionSubmissionCommentDto.getQuestionSubmissionCommentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionId);
        LtiUserEntity user = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        questionSubmissionCommentDto.setCreator(user.getDisplayName());
        QuestionSubmissionComment questionSubmissionComment;

        try {
            questionSubmissionComment = fromDto(questionSubmissionCommentDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create question submission comment: " + ex.getMessage(), ex);
        }

        return toDto(questionSubmissionCommentRepository.save(questionSubmissionComment));
    }

    @Override
    public void updateQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionCommentId, long experimentId, long submissionId, SecuredInfo securedInfo) throws DataServiceException {
        QuestionSubmissionComment questionSubmissionComment = getQuestionSubmissionComment(questionSubmissionCommentId);
        LtiUserEntity user = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (!user.getDisplayName().equals(questionSubmissionComment.getCreator())) {
            throw new DataServiceException("Error 122: Only the creator of a comment can edit their own comment.");
        }

        questionSubmissionComment.setComment(questionSubmissionCommentDto.getComment());
        questionSubmissionCommentRepository.saveAndFlush(questionSubmissionComment);
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
        Optional<QuestionSubmission> questionSubmission = questionSubmissionRepository.findById(questionSubmissionCommentDto.getQuestionSubmissionId());

        if (questionSubmission.isEmpty()) {
            throw new DataServiceException("The question submission for the question submission comment doesn't exist.");
        }

        questionSubmissionComment.setQuestionSubmission(questionSubmission.get());

        return questionSubmissionComment;
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        questionSubmissionCommentRepository.deleteByQuestionSubmissionCommentId(id);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long questionSubmissionCommentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions/{questionSubmissionId}/question_submission_comments/{questionSubmissionCommentId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, questionSubmissionCommentId).toUri());

        return headers;
    }

}
