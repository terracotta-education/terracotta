package edu.iu.terracotta.repository.integrations;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.integrations.IntegrationTokenLog;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationTokenLogRepository extends JpaRepository<IntegrationTokenLog, Long> {

    List<IntegrationTokenLog> findAllByIntegrationToken_Id(long integrationTokenId);
    Optional<IntegrationTokenLog> findByCode(String code);

}
