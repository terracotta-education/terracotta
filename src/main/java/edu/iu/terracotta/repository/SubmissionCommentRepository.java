package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {

    List<SubmissionComment> findBySubmission_SubmissionId(Long submissionId);

    Optional<SubmissionComment> findBySubmission_SubmissionIdAndSubmissionCommentId(Long submissionId, Long submissionCommentId);

    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(Long assessmentId, Long submissionId, Long submissionCommentId);
}