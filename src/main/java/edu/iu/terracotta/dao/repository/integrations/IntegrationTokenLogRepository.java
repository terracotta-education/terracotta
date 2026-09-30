package edu.iu.terracotta.dao.repository.integrations;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.integrations.IntegrationTokenLog;
import edu.iu.terracotta.dao.model.enums.integrations.IntegrationTokenStatus;


@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationTokenLogRepository extends JpaRepository<IntegrationTokenLog, Long> {

    List<IntegrationTokenLog> findAllByIntegrationToken_Id(long integrationTokenId);
    Optional<IntegrationTokenLog> findByCode(String code);
    boolean existsByTokenAndStatus(String token, IntegrationTokenStatus status);

}
