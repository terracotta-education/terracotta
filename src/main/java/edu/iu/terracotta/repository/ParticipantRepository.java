package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
  List<Participant> findByExperiment_ExperimentId(
      Long experimentId);

  Optional<Participant> findByParticipantIdAndExperiment_ExperimentId(Long participantId, Long experimentId);

  List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);

  Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);

  boolean existsByExperiment_ExperimentIdAndParticipantId(Long experimentId, Long participantId);


}