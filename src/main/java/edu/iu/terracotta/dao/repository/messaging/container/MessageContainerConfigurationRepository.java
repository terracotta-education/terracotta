package edu.iu.terracotta.dao.repository.messaging.container;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageContainerConfigurationRepository extends JpaRepository<MessageContainerConfiguration, Long> {

    Optional<MessageContainerConfiguration> findByUuid(UUID uuid);
    Optional<MessageContainerConfiguration> findByUuidAndContainer_Uuid(UUID uuid, UUID containerUuid);
    Optional<MessageContainerConfiguration> findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(UUID uuid, UUID containerUuid, String lmsUserId);

}
