package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.events.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}