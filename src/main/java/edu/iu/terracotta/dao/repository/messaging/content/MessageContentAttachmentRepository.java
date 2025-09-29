package edu.iu.terracotta.dao.repository.messaging.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageContentAttachmentRepository extends JpaRepository<MessageContentAttachment, Long> {

    Optional<MessageContentAttachment> findByUuid(UUID uuid);
    List<MessageContentAttachment> findAllByContent_Id(long contentId);
    List<MessageContentAttachment> findAllByUrl(String url);

}
