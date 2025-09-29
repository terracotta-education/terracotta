package edu.iu.terracotta.dao.repository.messaging.conditional;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageConditionalTextRepository extends JpaRepository<MessageConditionalText, Long> {

    Optional<MessageConditionalText> findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(UUID uuid, UUID contentUuid, String lmsUserId);

}
