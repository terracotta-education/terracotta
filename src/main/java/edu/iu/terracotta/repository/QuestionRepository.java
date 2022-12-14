package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByAssessment_AssessmentIdOrderByQuestionOrder(Long assessmentId);

    List<Question> findByAssessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId);

    Page<Question> findByAssessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId, Pageable pageable);

    boolean existsByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    Optional<Question> findByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    Question findByQuestionId(Long questionId);

    @Transactional
    void deleteByQuestionId(Long questionId);

}
