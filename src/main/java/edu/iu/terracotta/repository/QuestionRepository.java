package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByAssessment_AssessmentId(Long assessmentId);

    boolean existsByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

    Optional<Question> findByAssessment_AssessmentIdAndQuestionId(Long assessmentId, Long questionId);

}