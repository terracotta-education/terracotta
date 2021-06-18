package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssessment_AssessmentId(Long assessmentId);

    List<Submission> findByParticipant_ParticipantId(Long participantId);

    Optional<Submission> findByParticipant_ParticipantIdAndSubmissionId(Long participantId, Long submissionId);

    List<Submission> findByAssessment_Treatment_Assignment_AssignmentId(Long assignmentId);



    boolean existsByAssessment_AssessmentIdAndSubmissionId(Long assessmentId, Long submissionId);
}