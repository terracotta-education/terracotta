package edu.iu.terracotta.dao.repository.messaging.email;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;

public interface MessageEmailReplyToRepository extends JpaRepository<MessageEmailReplyTo, Long> {

}
