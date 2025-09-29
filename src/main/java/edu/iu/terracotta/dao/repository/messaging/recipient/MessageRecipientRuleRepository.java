package edu.iu.terracotta.dao.repository.messaging.recipient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageRecipientRuleRepository extends JpaRepository<MessageRecipientRule, Long> {

    Optional<MessageRecipientRule> findByUuidAndRuleSet_Uuid(UUID uuid, UUID ruleSetUuid);

}
