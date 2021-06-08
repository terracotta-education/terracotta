package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssessment_AssessmentId(Long assessmentId);

    boolean existsByAssessment_AssessmentIdAndSubmissionId(Long assessmentId, Long submissionId);
}