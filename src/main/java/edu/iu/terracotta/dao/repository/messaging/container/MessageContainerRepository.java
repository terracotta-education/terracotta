package edu.iu.terracotta.dao.repository.messaging.container;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface MessageContainerRepository extends JpaRepository<MessageContainer, Long> {

    Optional<MessageContainer> findByUuid(UUID uuid);
    Optional<MessageContainer> findByUuidAndExposure_ExposureIdAndOwner_LmsUserId(UUID uuid, long exposureId, String lmsUserId);
    List<MessageContainer> findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderAsc(long exposureId, String lmsUserId);
    List<MessageContainer> findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(long exposureId, String lmsUserId);
    List<MessageContainer> findAllByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOwner_LmsUserIdAndConfiguration_StatusInOrderByConfiguration_ContainerOrderAsc(long experimentId, long exposureId, String lmsUserId, List<MessageStatus> statuses);
    List<MessageContainer> findAllByExposure_Experiment_ExperimentIdAndOwner_LmsUserId(long experimentId, String lmsUserId);
    List<MessageContainer> findAllByConfiguration_StatusAndMessages_Configuration_SendAtLessThan(MessageStatus status, Timestamp sendAt);
    boolean existsByUuidAndOwner_LmsUserId(UUID messageUuid, String lmsUserId);
    boolean existsByExposure_ExposureId(long exposureId);

}
