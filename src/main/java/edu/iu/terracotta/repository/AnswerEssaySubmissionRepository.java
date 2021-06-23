package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerEssaySubmissionRepository extends JpaRepository<AnswerEssaySubmission, Long> {
    List<AnswerEssaySubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(Long questionSubmissionId, Long answerEssaySubmissionId);
}