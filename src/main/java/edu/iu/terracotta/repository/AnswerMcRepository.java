package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AnswerMcRepository extends JpaRepository<AnswerMc, Long> {

    List<AnswerMc> findByQuestion_QuestionId(Long questionId);

    Optional<AnswerMc> findByQuestion_QuestionIdAndAnswerMcId(Long questionId, Long answerMcId);

    boolean existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(Long assessmentId, Long questionId, Long answerMcId);

    @Transactional
    @Modifying
    @Query("delete from AnswerMc s where s.answerMcId = ?1")
    void deleteByAnswerMcId(Long answerMcId);
}