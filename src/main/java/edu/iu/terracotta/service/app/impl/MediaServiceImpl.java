package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.media.*;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.MediaService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.caliper.impl.CaliperServiceImpl;
import edu.iu.terracotta.service.common.Utils;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.context.JsonldContext;
import org.imsglobal.caliper.context.JsonldStringContext;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MediaServiceImpl implements MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

    public static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private CaliperServiceImpl caliperService;


    private String applicationName;
    private String applicationUrl;
    private final SoftwareApplication softwareApplication;
    private final JsonldContext context;

    @Autowired
    public MediaServiceImpl(@Value("${application.name}") final String applicationNameAttribute,
                            @Value("${application.url}") final String applicationUrlAttribute) {
        applicationName = applicationNameAttribute;
        applicationUrl = applicationUrlAttribute;
        context = JsonldStringContext.create(DATA_VERSION);
        softwareApplication = Utils.prepareSoftwareApplication(applicationName, applicationUrl);
    }

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
        mediaEventDto.setType(EventType.valueOf(mediaEvent.getEventType()));
        mediaEventDto.setAction(Action.valueOf(mediaEvent.getEventAction()));
        mediaEventDto.setProfile(mediaEvent.getEventProfile());
        mediaEventDto.setId(mediaEvent.getCaliperId());

        personDto.setType(EntityType.valueOf(mediaEvent.getActorType()));
        personDto.setId(mediaEvent.getActorId());
        mediaEventDto.setActor(personDto);

        groupDto.setId(mediaEvent.getEventGroup());
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
    public void fromDto(MediaEventDto mediaEventDto, SecuredInfo securedInfo,
                        Long experimentId, Long submissionId, Long questionId) throws ParameterMissingException, NoSubmissionsException {
        edu.iu.terracotta.model.events.Event event = new edu.iu.terracotta.model.events.Event();

        if (mediaEventDto.getEventTime() == null) {
            throw new ParameterMissingException("Event time is empty");
        }

        if (mediaEventDto.getAction() == null) {
            throw new ParameterMissingException("MediaEvent Action not found");
        }


        if (mediaEventDto.getObject() == null) {
            throw new ParameterMissingException("MediaEvent Object not found");
        }
        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);
        Participant participant = submission.getParticipant();
        caliperService.sendMediaEvent(mediaEventDto, participant, securedInfo, submission, questionId);
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
    public List<Event> findAllByEventType(String eventType) {
        return allRepositories.eventRepository.findByEventType(eventType);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.eventRepository.deleteById(id);
    }


}
