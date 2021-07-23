package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnswerMcRepository extends JpaRepository<AnswerMc, Long> {

    List<AnswerMc> findByQuestion_QuestionId(Long questionId);

    List<AnswerMc> findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId);

    AnswerMc findByAnswerMcId(Long answerMcId);

    boolean existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(Long assessmentId, Long questionId, Long answerMcId);

    @Transactional
    @Modifying
    @Query("delete from AnswerMc s where s.answerMcId = ?1")
    void deleteByAnswerMcId(Long answerMcId);
}