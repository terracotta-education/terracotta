package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AnswerEssaySubmissionRepository extends JpaRepository<AnswerEssaySubmission, Long> {

    List<AnswerEssaySubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    AnswerEssaySubmission findByAnswerEssaySubmissionId(Long answerEssaySubmissionId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(Long questionSubmissionId, Long answerEssaySubmissionId);

    @Modifying
    @Transactional
    @Query("delete from AnswerEssaySubmission s where s.answerEssaySubmissionId = ?1")
    void deleteByAnswerEssaySubmissionId(Long answerEssaySubmissionId);

}