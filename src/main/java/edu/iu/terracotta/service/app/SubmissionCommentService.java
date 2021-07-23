package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface SubmissionCommentService {

    List<SubmissionComment> findAllBySubmissionId(Long submissionId);

    List<SubmissionCommentDto> getSubmissionComments(Long submissionId);

    SubmissionComment getSubmissionComment(Long id);

    void updateSubmissionComment(SubmissionComment submissionComment, SubmissionCommentDto submissionCommentDto);

    SubmissionCommentDto toDto(SubmissionComment submissionComment);

    SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException;

    SubmissionComment save(SubmissionComment submissionComment);

    Optional<SubmissionComment> findById(Long id);

    Optional<SubmissionComment> findBySubmissionIdAndSubmissionCommentId(Long submissionId, Long submissionCommentId);

    LtiUserEntity findByUserKey(String key);

    void saveAndFlush(SubmissionComment submissionCommentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean submissionCommentBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long submissionCommendId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId, long submissionCommentId);
}
