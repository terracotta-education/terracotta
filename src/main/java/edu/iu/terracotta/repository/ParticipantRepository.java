package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
  List<Participant> findByExperiment_ExperimentId(Long experimentId);

  Optional<Participant> findByParticipantIdAndExperiment_ExperimentId(Long participantId, Long experimentId);

  List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);

  List<Participant> findByExperiment_ExperimentIdAndConsent(Long experimentId, Boolean consent);

  Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);

  Participant findByParticipantId(Long participantId);

  boolean existsByExperiment_ExperimentIdAndParticipantId(Long experimentId, Long participantId);

  List<Participant> findByGroup_GroupId(Long groupId);

  long countDistinctByGroup_GroupId(Long groupId);





  @Transactional
  @Modifying
  @Query("delete from Participant s where s.participantId = ?1")
  void deleteByParticipantId(Long participantId);

  long countByGroup_GroupId(Long groupId);



}