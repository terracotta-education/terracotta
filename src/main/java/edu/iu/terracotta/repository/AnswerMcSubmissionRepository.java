package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerMcSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AnswerMcSubmissionRepository extends JpaRepository<AnswerMcSubmission, Long> {

    List<AnswerMcSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);

    AnswerMcSubmission findByAnswerMcSubId(Long answerMcSubId);

    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(Long questionSubmissionId, Long answerMcSubId);

    @Transactional
    void deleteByAnswerMcSubId(Long answerMcSubId);

}