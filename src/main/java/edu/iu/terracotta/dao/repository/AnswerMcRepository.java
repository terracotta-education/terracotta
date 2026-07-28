package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.AnswerMc;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AnswerMcRepository extends JpaRepository<AnswerMc, Long> {

    List<AnswerMc> findByQuestion_QuestionId(Long questionId);
    @Query("SELECT a FROM AnswerMc a WHERE a.question.assessment.treatment.condition.experiment.experimentId = :experimentId")
    List<AnswerMc> findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(@Param("experimentId") Long experimentId);
    Page<AnswerMc> findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId, Pageable pageable);
    AnswerMc findByAnswerMcId(Long answerMcId);
    Optional<AnswerMc> findByQuestion_QuestionIdAndAnswerMcId(Long questionId, Long answerMcId);
    boolean existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(Long assessmentId, Long questionId, Long answerMcId);

    @Transactional
    void deleteByAnswerMcId(Long answerMcId);
}