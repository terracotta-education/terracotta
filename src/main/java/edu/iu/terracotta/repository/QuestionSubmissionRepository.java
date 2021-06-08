package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.QuestionSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, Long> {

    List<QuestionSubmission> findBySubmission_SubmissionId(Long submissionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(Long assessmentId, Long submissionId, Long questionSubmissionId);
}