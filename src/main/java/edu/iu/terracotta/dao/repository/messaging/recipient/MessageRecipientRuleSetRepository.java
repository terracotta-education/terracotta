package edu.iu.terracotta.dao.repository.messaging.recipient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageRecipientRuleSetRepository extends JpaRepository<MessageRecipientRuleSet, Long> {

    Optional<MessageRecipientRuleSet> findByUuidAndMessage_Uuid(UUID uuid, UUID messageUuid);

}
