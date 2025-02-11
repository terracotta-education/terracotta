package edu.iu.terracotta.dao.repository.integrations;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;


@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationTokenRepository extends JpaRepository<IntegrationToken, Long> {

    Optional<IntegrationToken> findByToken(String token);
    Optional<IntegrationToken> findBySubmission_SubmissionId(long submissionId);

}
