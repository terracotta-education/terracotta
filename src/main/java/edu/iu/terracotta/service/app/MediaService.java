package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.events.Event;


import java.util.List;
import java.util.Optional;

public interface MediaService {

    MediaEventDto toDto(Event mediaEvent);

    edu.iu.terracotta.model.events.Event fromDto(MediaEventDto mediaEventDto) throws ParameterMissingException;

    Event save(Event mediaEvent);

    Optional<Event> findById(Long id);

    List<Event> getAllEvents();

    List<Event> findAllByType(String type);

    void deleteById(Long id);

}
