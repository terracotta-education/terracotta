package edu.iu.terracotta.dao.repository.messaging.message;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByUuid(UUID uuid);
    boolean existsByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(UUID messageUuid, UUID containerUuid, String lmsUserId);
    Optional<Message> findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(UUID messageUuid, UUID containerUuid, String lmsUserId);
    List<Message> findByConfiguration_StatusAndConfiguration_SendAtLessThan(MessageStatus messageStatus, Timestamp sendAt);
    List<Message> findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus containerStatus, MessageStatus messageStatus);

}
