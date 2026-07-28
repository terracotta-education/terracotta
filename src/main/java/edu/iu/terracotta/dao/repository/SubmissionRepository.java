package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Submission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssessment_AssessmentId(long assessmentId);
    long countByAssessment_AssessmentId(long assessmentId);
    List<Submission> findByParticipant_Id(long participantId);
    List<Submission> findByParticipant_Experiment_ExperimentId(long experimentId);
    Page<Submission> findByParticipant_Experiment_ExperimentId(long experimentId, Pageable pageable);
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assessment.treatment.assignment.assignmentId = :assignmentId")
    long countByAssessment_Treatment_Assignment_AssignmentId(@Param("assignmentId") long assignmentId);
    Optional<Submission> findByParticipant_IdAndSubmissionId(long participantId, long submissionId);
    Submission findBySubmissionId(long submissionId);
    List<Submission> findByParticipant_IdAndAssessment_AssessmentId(long participantId, long assessmentId);
    List<Submission> findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(long participantId, long assessmentId);
    List<Submission> findByParticipant_IdInAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(Collection<Long> participantIds, long assessmentId);
    @Query("SELECT s FROM Submission s WHERE s.assessment.treatment.assignment.assignmentId = :assignmentId")
    List<Submission> findByAssessment_Treatment_Assignment_AssignmentId(@Param("assignmentId") long assignmentId);
    long countByAssessment_Treatment_TreatmentId(long treatmentId);
    boolean existsByAssessment_AssessmentIdAndSubmissionId(long assessmentId, long submissionId);
    @Query("SELECT s FROM Submission s WHERE s.participant.experiment.experimentId = :experimentId AND s.dateSubmitted IS NOT NULL ORDER BY s.dateSubmitted DESC LIMIT 1")
    Optional<Submission> findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(@Param("experimentId") long experimentId);
    @Query("SELECT s FROM Submission s WHERE s.assessment.treatment.assignment.assignmentId = :assignmentId AND s.dateSubmitted IS NOT NULL ORDER BY s.dateSubmitted DESC LIMIT 1")
    Optional<Submission> findTopByAssessment_Treatment_Assignment_AssignmentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(@Param("assignmentId") long assignmentId);
    Optional<Submission> findByIntegrationToken_Token(String token);

    @Modifying
    @Transactional
    @Query("delete from Submission s where s.submissionId = ?1")
    void deleteBySubmissionId(long submissionId);

}
