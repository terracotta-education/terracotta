package edu.iu.terracotta.dao.repository.messaging.piped;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;

public interface PipedTextItemValueRepository extends JpaRepository<MessagePipedTextItemValue, Long> {

    Optional<MessagePipedTextItemValue> findByUuid(UUID uuid);

}
