package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Participant;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
  List<Participant> findByExperiment_ExperimentId(
      Long experimentId);

  Participant findByParticipantIdAndExperiment_ExperimentId(Long participantId, Long experimentId);

  List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);



  boolean existsByExperiment_ExperimentIdAndParticipantId(Long experimentId, Long participantId);


}