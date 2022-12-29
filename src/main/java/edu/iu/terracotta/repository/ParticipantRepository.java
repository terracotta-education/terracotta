package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.Participant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S100", "PMD.MethodNamingConventions"})
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    long countByExperiment_ExperimentId(Long experimentId);

    Page<Participant> findByExperiment_ExperimentId(Long experimentId, Pageable pageable);

    List<Participant> findByExperiment_ExperimentId(Long experimentId);

    Optional<Participant> findByParticipantIdAndExperiment_ExperimentId(Long participantId, Long experimentId);

    List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);

    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);

    Participant findByParticipantId(Long participantId);

    boolean existsByExperiment_ExperimentIdAndParticipantId(Long experimentId, Long participantId);

    List<Participant> findByGroup_GroupId(Long groupId);

    long countDistinctByGroup_GroupId(Long groupId);

    @Transactional
    void deleteByParticipantId(Long participantId);

    long countByGroup_GroupId(Long groupId);

}
