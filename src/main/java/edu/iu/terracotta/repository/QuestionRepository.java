package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByAssessment_AssessmentIdOrderByQuestionId(Long assessmentId);

    List<Question> findByAssessment_Treatment_Condition_Experiment_ExperimentId(Long experimentId);



    boolean existsByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    Optional<Question> findByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    Question findByQuestionId(Long questionId);

    @Transactional
    @Modifying
    @Query("delete from Question s where s.questionId = ?1")
    void deleteByQuestionId(Long questionId);

}