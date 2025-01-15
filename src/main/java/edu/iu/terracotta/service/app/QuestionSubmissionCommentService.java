package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface QuestionSubmissionCommentService {

    List<QuestionSubmissionCommentDto> getQuestionSubmissionComments(Long questionSubmissionId);
    QuestionSubmissionComment getQuestionSubmissionComment(Long id);
    QuestionSubmissionCommentDto postQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;
    void updateQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionCommentId, long experimentId, long submissionId, SecuredInfo securedInfo) throws DataServiceException;
    QuestionSubmissionCommentDto toDto(QuestionSubmissionComment questionSubmissionComment);
    QuestionSubmissionComment fromDto(QuestionSubmissionCommentDto questionSubmissionCommentDto) throws DataServiceException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long questionSubmissionCommentId);

}
