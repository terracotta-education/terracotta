package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.Participant;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByExperiment_ExperimentId(Long experimentId);
    long countByExperiment_ExperimentId(Long experimentId);
    Page<Participant> findByExperiment_ExperimentId(Long experimentId, Pageable pageable);
    Optional<Participant> findByParticipantIdAndExperiment_ExperimentId(Long participantId, Long experimentId);
    List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);
    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);
    Participant findByParticipantId(Long participantId);
    boolean existsByExperiment_ExperimentIdAndParticipantId(Long experimentId, Long participantId);
    List<Participant> findByGroup_GroupId(Long groupId);
    long countDistinctByGroup_GroupId(Long groupId);
    long countByGroup_GroupId(Long groupId);

    @Modifying
    @Transactional
    @Query("delete from Participant s where s.participantId = ?1")
    void deleteByParticipantId(Long participantId);

}
