package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface QuestionSubmissionCommentService {

    List<QuestionSubmissionComment> findAllByQuestionSubmissionId(Long questionSubmissionId);

    List<QuestionSubmissionCommentDto> getQuestionSubmissionComments(Long questionSubmissionId);

    QuestionSubmissionComment getQuestionSubmissionComment(Long id);

    QuestionSubmissionCommentDto postQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException;

    void updateQuestionSubmissionComment(QuestionSubmissionCommentDto questionSubmissionCommentDto, long questionSubmissionCommentId, long experimentId, long submissionId, SecuredInfo securedInfo)
        throws DataServiceException;

    QuestionSubmissionCommentDto toDto(QuestionSubmissionComment questionSubmissionComment);

    QuestionSubmissionComment fromDto(QuestionSubmissionCommentDto questionSubmissionCommentDto) throws DataServiceException;

    QuestionSubmissionComment save(QuestionSubmissionComment questionSubmissionComment);

    Optional<QuestionSubmissionComment> findById(Long id);

    void saveAndFlush(QuestionSubmissionComment questionSubmissionCommentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean questionSubmissionCommentBelongsToQuestionSubmission(Long questionSubmissionId, Long questionSubmissionCommentId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long submissionId, Long questionSubmissionId, Long questionSubmissionCommentId);

}
