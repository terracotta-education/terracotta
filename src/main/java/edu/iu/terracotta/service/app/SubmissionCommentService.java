package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface SubmissionCommentService {

    List<SubmissionComment> findAllBySubmissionId(Long submissionId);

    SubmissionCommentDto toDto(SubmissionComment submissionComment);

    SubmissionComment fromDto(SubmissionCommentDto submissionCommentDto) throws DataServiceException;

    SubmissionComment save(SubmissionComment submissionComment);

    Optional<SubmissionComment> findById(Long id);

    Optional<SubmissionComment> findBySubmissionIdAndSubmissionCommentId(Long submissionId, Long submissionCommentId);

    LtiUserEntity findByUserKey(String key);

    void saveAndFlush(SubmissionComment submissionCommentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean submissionCommentBelongsToAssessmentAndSubmission(Long assessmentId, Long submissionId, Long submissionCommendId);
}
