package edu.iu.terracotta.dao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.iu.terracotta.dao.entity.events.Event;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.participant.experiment.experimentId = :experimentId")
    List<Event> findByParticipant_Experiment_ExperimentId(@Param("experimentId") Long experimentId);
    Page<Event> findByParticipant_Experiment_ExperimentId(Long experimentId, Pageable pageable);
    List<Event> findByType(String type);

}
