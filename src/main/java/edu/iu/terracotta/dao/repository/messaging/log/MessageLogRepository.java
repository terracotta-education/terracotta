package edu.iu.terracotta.dao.repository.messaging.log;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageLogRepository extends JpaRepository <MessageLog, Long> {

    List<MessageLog> findAllByMessage_Id(long messageId);
    List<MessageLog> findAllByMessage_IdIn(List<Long> messageIds);
    @Query("SELECT ml FROM MessageLog ml WHERE ml.message.exposureGroupCondition.condition.experiment.experimentId = :experimentId ORDER BY ml.createdAt DESC LIMIT 1")
    Optional<MessageLog> findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(@Param("experimentId") long experimentId);

}
