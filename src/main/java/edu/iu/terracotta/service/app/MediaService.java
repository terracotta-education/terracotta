package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import java.util.List;
import java.util.Optional;

public interface MediaService {

    MediaEventDto toDto(Event mediaEvent);

    void fromDto(MediaEventDto mediaEventDto, SecuredInfo securedInfo,
                                                 Long experimentId, Long submissionId, Long questionId) throws ParameterMissingException, NoSubmissionsException;

    Event save(Event mediaEvent);

    Optional<Event> findById(Long id);

    List<Event> getAllEvents();

    List<Event> findAllByType(String type);

    void deleteById(Long id);

}
