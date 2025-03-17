package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.AnswerFileSubmission;

import java.util.List;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface AnswerFileSubmissionRepository extends JpaRepository<AnswerFileSubmission, Long> {

    List<AnswerFileSubmission> findByQuestionSubmission_QuestionSubmissionId(Long questionSubmissionId);
    List<AnswerFileSubmission> findByQuestionSubmission_Question_QuestionId(Long questionId);
    AnswerFileSubmission findByAnswerFileSubmissionId(Long answerFileSubmissionId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndAnswerFileSubmissionId(Long questionSubmissionId, Long answerFileSubmissionId);

    @Transactional
    void deleteByAnswerFileSubmissionId(Long answerFileSubmissionId);

}
