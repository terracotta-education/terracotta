package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AnswerMcRepository extends JpaRepository<AnswerMc, Long> {

    List<AnswerMc> findByQuestion_QuestionId(Long questionId);

    List<AnswerMc> findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId);

    Page<AnswerMc> findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId, Pageable pageable);

    AnswerMc findByAnswerMcId(Long answerMcId);

    Optional<AnswerMc> findByQuestion_QuestionIdAndAnswerMcId(Long questionId, Long answerMcId);

    boolean existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(Long assessmentId, Long questionId, Long answerMcId);

    @Transactional
    void deleteByAnswerMcId(Long answerMcId);

}
