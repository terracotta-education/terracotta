package edu.iu.terracotta.dao.repository.messaging.piped;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface PipedTextRepository extends JpaRepository<MessagePipedText, Long> {

    Optional<MessagePipedText> findByUuid(UUID uuid);
    Optional<MessagePipedText> findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(UUID uuid, UUID contentUuid, String lmsUserId);

}
