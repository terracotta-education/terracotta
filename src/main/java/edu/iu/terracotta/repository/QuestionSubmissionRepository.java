package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.QuestionSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, Long> {

    List<QuestionSubmission> findBySubmission_SubmissionId(Long submissionId);

    List<QuestionSubmission> findBySubmission_Participant_Experiment_ExperimentId(Long experimentId);

    QuestionSubmission findByQuestionSubmissionId(Long questionSubmissionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndQuestion_QuestionId(Long assessmentId, Long questionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(Long assessmentId, Long submissionId, Long questionSubmissionId);

    @Transactional
    @Modifying
    @Query("delete from QuestionSubmission s where s.questionSubmissionId = ?1")
    void deleteByQuestionSubmissionId(Long questionSubmissionId);
}