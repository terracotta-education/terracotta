package edu.iu.terracotta.dao.repository.messaging.message;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageConfigurationRepository extends JpaRepository<MessageConfiguration, Long> {

    Optional<MessageConfiguration> findByUuid(UUID uuid);
    Optional<MessageConfiguration> findByMessage_Id(long messageId);
    Optional<MessageConfiguration> findByUuidAndMessage_Uuid(UUID uuid, UUID messageUuid);
    Optional<MessageConfiguration> findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(UUID uuid, UUID messageUuid, String lmsUserId);

}
