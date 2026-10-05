package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

public interface SubmissionCommentService {

    List<SubmissionCommentDto> getSubmissionComments(Long submissionId);
    SubmissionComment getSubmissionComment(Long id);
    SubmissionComment getSubmissionCommentByUuid(UUID uuid) throws SubmissionCommentNotMatchingException;
    long getSubmissionCommentIdByUuid(UUID uuid) throws SubmissionCommentNotMatchingException;
    SubmissionCommentDto postSubmissionComment(SubmissionCommentDto submissionCommentDto, long submissionId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;
    void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto);
    SubmissionCommentDto toDto(SubmissionComment submissionComment);
    SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, UUID experimentId, UUID conditionId, UUID treatmentId, UUID assessmentId, UUID submissionId, UUID submissionCommentId);

}
