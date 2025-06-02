package edu.iu.terracotta.dao.repository.messaging.content;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageContentRepository extends JpaRepository<MessageContent, Long> {

    Optional<MessageContent> findByUuid(UUID uuid);
    Optional<MessageContent> findByUuidAndMessage_Uuid(UUID uuid, UUID messageUuid);
    Optional<MessageContent> findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(UUID uuid, UUID messageUuid, String lmsUserId);

}
