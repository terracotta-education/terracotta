package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.model.app.dto.media.*;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.MediaService;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Component
public class MediaServiceImpl implements MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

    @Autowired
    private AllRepositories allRepositories;

    @Override
    public MediaEventDto toDto(Event mediaEvent) {
        MediaEventDto mediaEventDto = new MediaEventDto();
        PersonDto personDto = new PersonDto();
        GroupDto groupDto = new GroupDto();
        MediaObjectDto mediaObjectDto = new MediaObjectDto();
        MediaLocationDto mediaLocationDto = new MediaLocationDto();
        MembershipDto membershipDto = new MembershipDto();
        SessionDto sessionDto = new SessionDto();

        mediaEventDto.setContext(mediaEvent.getLtiContextId());
        mediaEventDto.setType(EventType.valueOf(mediaEvent.getType()));
        mediaEventDto.setAction(Action.valueOf(mediaEvent.getAction()));
        mediaEventDto.setProfile(mediaEvent.getProfile());
        mediaEventDto.setId(mediaEvent.getCaliperId());

        personDto.setType(EntityType.valueOf(mediaEvent.getActorType()));
        personDto.setId(mediaEvent.getActorId());
        mediaEventDto.setActor(personDto);

        groupDto.setId(mediaEvent.getGroup());
        mediaEventDto.setGroup(groupDto);

        mediaObjectDto.setId(mediaEvent.getObjectId());
        mediaObjectDto.setMediaType(mediaEvent.getObjectType());
        mediaEventDto.setObject(mediaObjectDto);

        mediaLocationDto.setId(mediaEvent.getTargetId());
        mediaLocationDto.setType(EntityType.valueOf(mediaEvent.getTargetType()));
        mediaEventDto.setTarget(mediaLocationDto);

        membershipDto.setId(mediaEvent.getMembershipId());
        membershipDto.setRoles(new String[]{mediaEvent.getMembershipRoles()});
        mediaEventDto.setMembership(membershipDto);

        sessionDto.setId(mediaEvent.getFederatedSession());
        mediaEventDto.setSession(sessionDto);

        return mediaEventDto;
    }

    @Override
    public edu.iu.terracotta.model.events.Event fromDto(MediaEventDto mediaEventDto) throws ParameterMissingException {
        edu.iu.terracotta.model.events.Event event = new edu.iu.terracotta.model.events.Event();
        if (mediaEventDto.getId() == null || mediaEventDto.getId().isEmpty()) {
            throw new ParameterMissingException("MediaEvent Id not found");
        }
        if (mediaEventDto.getEventTime() == null) {
            throw new ParameterMissingException("Event time is empty");
        }
        if (mediaEventDto.getActor() == null) {
            throw new ParameterMissingException("MediaEvent Actor not found");
        }
        if (mediaEventDto.getAction() == null) {
            throw new ParameterMissingException("MediaEvent Action not found");
        }
        if (mediaEventDto.getGroup() == null) {
            throw new ParameterMissingException("MediaEvent Group not found");
        }

        if (mediaEventDto.getObject() == null) {
            throw new ParameterMissingException("MediaEvent Object not found");
        }

        if (mediaEventDto.getTarget() == null) {
            throw new ParameterMissingException("MediaEvent Target not found");
        }

        if (mediaEventDto.getMembership() == null) {
            throw new ParameterMissingException("MediaEvent Membership not found");
        }

        if (mediaEventDto.getMembership().getRoles().length == 0) {
            throw new ParameterMissingException("MediaEvent Membership  roles not found");
        }

        if (mediaEventDto.getSession() == null) {
            throw new ParameterMissingException("MediaEvent Session not found");
        }
        event.setCaliperId(mediaEventDto.getId());
        event.setEventTime(new Timestamp(mediaEventDto.getEventTime().getMillis()));
        event.setActorId(mediaEventDto.getActor().getId());
        event.setActorType(mediaEventDto.getActor().getType().name());
        event.setType(mediaEventDto.getType().name());
        event.setProfile(mediaEventDto.getProfile());
        event.setAction(mediaEventDto.getAction().name());
        event.setGroup(mediaEventDto.getGroup().getId());
        event.setObjectId(mediaEventDto.getObject().getId());
        event.setObjectType(mediaEventDto.getObject().getMediaType());
        event.setTargetId(mediaEventDto.getTarget().getId());
        event.setTargetType(mediaEventDto.getTarget().getType().name());
        event.setMembershipId(mediaEventDto.getMembership().getId());
        event.setMembershipRoles(mediaEventDto.getMembership().getRoles()[0]);
        event.setFederatedSession(mediaEventDto.getSession().getId());
        event.setLtiContextId(mediaEventDto.getContext());
        return event;
    }

    @Override
    public Event save(Event mediaEvent) {
        allRepositories.eventRepository.save(mediaEvent);
        return mediaEvent;
    }

    @Override
    public Optional<Event> findById(Long id) {
        return allRepositories.eventRepository.findById(id);
    }

    @Override
    public List<Event> getAllEvents() {
        return allRepositories.eventRepository.findAll();
    }

    @Override
    public List<Event> findAllByType(String type) {
        return allRepositories.eventRepository.findByType(type);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.eventRepository.deleteById(id);
    }


}
