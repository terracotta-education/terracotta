package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface SubmissionCommentService {

    List<SubmissionCommentDto> getSubmissionComments(Long submissionId);

    SubmissionComment getSubmissionComment(Long id);

    SubmissionCommentDto postSubmissionComment(SubmissionCommentDto submissionCommentDto, long submissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;

    void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto);

    SubmissionCommentDto toDto(SubmissionComment submissionComment);

    SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException;

    void deleteById(Long id) throws EmptyResultDataAccessException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId, long submissionCommentId);
}
