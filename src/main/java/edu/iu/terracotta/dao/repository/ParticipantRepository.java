package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.iu.terracotta.dao.entity.Participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByUuid(UUID uuid);
    List<Participant> findByExperiment_ExperimentId(Long experimentId);
    long countByExperiment_ExperimentId(Long experimentId);
    Page<Participant> findByExperiment_ExperimentId(Long experimentId, Pageable pageable);
    Optional<Participant> findByIdAndExperiment_ExperimentId(Long id, Long experimentId);
    List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);
    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);
    boolean existsByExperiment_ExperimentIdAndId(Long experimentId, Long id);
    List<Participant> findByGroup_GroupId(Long groupId);
    long countDistinctByGroup_GroupId(Long groupId);
    long countByGroup_GroupId(Long groupId);

}
