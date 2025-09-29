package edu.iu.terracotta.dao.repository.messaging.log;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageLogRepository extends JpaRepository <MessageLog, Long> {

    List<MessageLog> findAllByMessage_Id(long messageId);
    Optional<MessageLog> findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(long experimentId);

}
