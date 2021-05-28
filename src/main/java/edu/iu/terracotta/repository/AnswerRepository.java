package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestion_QuestionId(Long questionId);

    boolean existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerId(Long assessmentId, Long questionId, Long answerId);
}