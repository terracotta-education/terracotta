package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

public interface QuestionSubmissionCommentService {

    List<QuestionSubmissionCommentDto> getQuestionSubmissionComments(Long questionSubmissionId);
    QuestionSubmissionComment getQuestionSubmissionComment(Long id);
    QuestionSubmissionComment getQuestionSubmissionCommentByUuid(UUID uuid) throws QuestionSubmissionCommentNotMatchingException;
    long getQuestionSubmissionCommentIdByUuid(UUID uuid) throws QuestionSubmissionCommentNotMatchingException;
    QuestionSubmissionCommentDto postQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;
    void updateQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionCommentId, long experimentId, long submissionId, SecuredInfo securedInfo) throws DataServiceException;
    QuestionSubmissionCommentDto toDto(QuestionSubmissionComment questionSubmissionComment);
    QuestionSubmissionComment fromDto(QuestionSubmissionCommentDto questionSubmissionCommentDto) throws DataServiceException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, UUID experimentId, UUID conditionId, UUID treatmentId, UUID assessmentId, UUID submissionId, UUID questionSubmissionId, UUID questionSubmissionCommentId);

}
