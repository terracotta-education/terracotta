package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.QuestionSubmission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, Long> {

    List<QuestionSubmission> findBySubmission_SubmissionId(Long submissionId);

    List<QuestionSubmission> findBySubmission_Participant_Experiment_ExperimentId(Long experimentId);

    Page<QuestionSubmission> findBySubmission_Participant_Experiment_ExperimentId(Long experimentId, Pageable pageable);

    QuestionSubmission findByQuestionSubmissionId(Long questionSubmissionId);

    Optional<QuestionSubmission> findByQuestion_QuestionIdAndSubmission_SubmissionId(long questionId, long submissionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndQuestion_QuestionId(Long assessmentId, Long questionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestion_QuestionId(Long assessmentId, Long submissionId, Long questionId);

    boolean existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(Long assessmentId, Long submissionId, Long questionSubmissionId);

    @Transactional
    void deleteByQuestionSubmissionId(Long questionSubmissionId);

}
