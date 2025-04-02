package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Submission;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssessment_AssessmentId(long assessmentId);
    long countByAssessment_AssessmentId(long assessmentId);
    List<Submission> findByParticipant_ParticipantId(long participantId);
    List<Submission> findByParticipant_Experiment_ExperimentId(long experimentId);
    Page<Submission> findByParticipant_Experiment_ExperimentId(long experimentId, Pageable pageable);
    long countByAssessment_Treatment_Assignment_AssignmentId(long assignmentId);
    Optional<Submission> findByParticipant_ParticipantIdAndSubmissionId(long participantId, long submissionId);
    Submission findBySubmissionId(long submissionId);
    List<Submission> findByParticipant_ParticipantIdAndAssessment_AssessmentId(long participantId, long assessmentId);
    List<Submission> findByParticipant_ParticipantIdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(long participantId, long assessmentId);
    List<Submission> findByAssessment_Treatment_Assignment_AssignmentId(long assignmentId);
    long countByAssessment_Treatment_TreatmentId(long treatmentId);
    boolean existsByAssessment_AssessmentIdAndSubmissionId(long assessmentId, long submissionId);
    Optional<Submission> findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(long experimentId);
    Optional<Submission> findTopByAssessment_Treatment_Assignment_AssignmentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(long assignmentId);

    @Modifying
    @Transactional
    @Query("delete from Submission s where s.submissionId = ?1")
    void deleteBySubmissionId(long submissionId);

}
