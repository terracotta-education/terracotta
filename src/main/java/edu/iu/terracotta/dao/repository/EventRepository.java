package edu.iu.terracotta.dao.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.events.Event;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByParticipant_Experiment_ExperimentId(Long experimentId);
    Page<Event> findByParticipant_Experiment_ExperimentId(Long experimentId, Pageable pageable);
    List<Event> findByType(String type);

}
