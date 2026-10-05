package edu.iu.terracotta.dao.repository.integrations;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.integrations.AnswerIntegrationSubmission;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AnswerIntegrationSubmissionRepository extends JpaRepository<AnswerIntegrationSubmission, Long> {

    AnswerIntegrationSubmission findByUuid(UUID uuid);

    List<AnswerIntegrationSubmission> findByQuestionSubmission_QuestionSubmissionId(long questionSubmissionId);
    boolean existsByQuestionSubmission_QuestionSubmissionIdAndId(long questionSubmissionId, long id);

}
