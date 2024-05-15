package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerFileSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AnswerFileSubmissionRepository extends JpaRepository<AnswerFileSubmission, Long> {

    List<AnswerFileSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    AnswerFileSubmission findByAnswerFileSubmissionId(Long answerFileSubmissionId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerFileSubmissionId(Long questionSubmissionId, Long answerFileSubmissionId);

    @Transactional
    void deleteByAnswerFileSubmissionId(Long answerFileSubmissionId);

}
