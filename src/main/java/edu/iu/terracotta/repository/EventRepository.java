package edu.iu.terracotta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.events.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByParticipant_Experiment_ExperimentId(Long experimentId);
}
