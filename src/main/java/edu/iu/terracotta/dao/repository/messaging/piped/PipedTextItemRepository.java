package edu.iu.terracotta.dao.repository.messaging.piped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;

@SuppressWarnings("PMD.MethodNamingConventions")
public interface PipedTextItemRepository extends JpaRepository<MessagePipedTextItem, Long> {

    Optional<MessagePipedTextItem> findByUuid(UUID uuid);
    Optional<MessagePipedTextItem> findByUuidAndPipedText_UuidAndPipedText_Content_UuidAndPipedText_Content_Message_Container_Owner_LmsUserId(UUID uuid, UUID pipedTextUuid, UUID contentUuid, String lmsUserId);
    List<MessagePipedTextItem> findByPipedText_Id(long pipedTextId);

}
