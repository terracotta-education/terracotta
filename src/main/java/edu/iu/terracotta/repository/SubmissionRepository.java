package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssessment_AssessmentId(Long assessmentId);

    List<Submission> findByParticipant_ParticipantId(Long participantId);

    Optional<Submission> findByParticipant_ParticipantIdAndSubmissionId(Long participantId, Long submissionId);

    List<Submission> findByAssessment_Treatment_Assignment_AssignmentId(Long assignmentId);

    @Transactional
    @Modifying
    @Query("delete from Submission s where s.submissionId = ?1")
    void deleteBySubmissionId(Long submissionId);

    boolean existsByAssessment_AssessmentIdAndSubmissionId(Long assessmentId, Long submissionId);
}