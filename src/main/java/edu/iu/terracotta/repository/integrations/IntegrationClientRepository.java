package edu.iu.terracotta.repository.integrations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.integrations.IntegrationClient;

public interface IntegrationClientRepository extends JpaRepository<IntegrationClient, Long> {

    Optional<IntegrationClient> findByUuid(UUID uuid);
    List<IntegrationClient> getAllByEnabled(boolean enabled);
    Optional<IntegrationClient> findByPreviewToken(String previewToken);

}
