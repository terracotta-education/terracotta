package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {

    List<SubmissionComment> findBySubmission_SubmissionId(Long submissionId);
    Optional<SubmissionComment> findBySubmission_SubmissionIdAndSubmissionCommentId(Long submissionId, Long submissionCommentId);
    SubmissionComment findBySubmissionCommentId(Long submissionCommentId);
    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(Long assessmentId, Long submissionId, Long submissionCommentId);

    @Modifying
    @Transactional
    @Query("delete from SubmissionComment s where s.submissionCommentId = ?1")
    void deleteBySubmissionCommentId(Long submissionCommentId);

}
