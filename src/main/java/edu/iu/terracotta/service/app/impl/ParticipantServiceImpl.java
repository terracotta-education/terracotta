package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.dto.UserDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ConsentDocumentRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.GroupRepository;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.GroupParticipantService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@SuppressWarnings({"PMD.UselessParentheses", "PMD.GuardLogStatement", "PMD.PreserveStackTrace", "squid:S112", "squid:S1066"})
public class ParticipantServiceImpl implements ParticipantService {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AdvantageAGSService advantageAGSService;
    @Autowired private AdvantageMembershipService advantageMembershipService;
    @Autowired private APIJWTService apijwtService;
    @Autowired private CanvasAPIClient canvasAPIClient;
    @Autowired private GroupParticipantService groupParticipantService;
    @Autowired private LTIDataService ltiDataService;

    @Override
    public List<Participant> findAllByExperimentId(long experimentId) {
        return participantRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<ParticipantDto> getParticipants(List<Participant> participants, long experimentId, String userId, boolean student, SecuredInfo securedInfo) {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        // retrieve published assignment IDs from Canvas
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getCanvasCourseId(), experiment.getCreatedBy());

        if (!student) {
            if (CollectionUtils.isEmpty(participants)) {
                log.info("No participants passed in for exeriment ID: [{}]. Retrieving from database.", experimentId);
                participants = findAllByExperimentId(experimentId);
            }

            return participants.stream()
                .filter(participant -> !participant.isTestStudent())
                .map(participant -> toDto(participant, publishedExperimentAssignmentIds, securedInfo))
                .toList();
        }

        try {
            return Collections.singletonList(
                toDto(
                    participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId),
                    publishedExperimentAssignmentIds,
                    securedInfo
                )
            );
        } catch (NullPointerException ex) {
            // NPE == no participant for this experiment with that userId; return an empty list
            return Collections.emptyList();
        }
    }

    @Override
    public Participant getParticipant(long participantId, long experimentId, String userId, boolean student) throws InvalidUserException {
        if (!student) {
            return participantRepository.findByParticipantId(participantId);
        }

        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);

        if (!participant.getParticipantId().equals(participantId)) {
            throw new InvalidUserException("Error 146: Students are not authorized to view other participants.");
        }

        return participantRepository.findByParticipantId(participantId);
    }

    @Override
    public ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getCanvasCourseId(), experiment.getCreatedBy());

        if (participantDto.getParticipantId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        Participant participant;
        participantDto.setExperimentId(experimentId);

        try {
            participant = fromDto(participantDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create the participant:" + e.getMessage(), e);
        }

        return toDto(participantRepository.save(participant), publishedExperimentAssignmentIds, securedInfo);
    }

    @Override
    public ParticipantDto toDto(Participant participant, SecuredInfo securedInfo) {
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(participant.getExperiment().getExperimentId(), securedInfo.getCanvasCourseId(), participant.getExperiment().getCreatedBy());

        return toDto(participant, publishedExperimentAssignmentIds, securedInfo);
    }

    @Override
    public ParticipantDto toDto(Participant participant, List<Long> publishedExperimentAssignmentIds, SecuredInfo securedInfo) {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setParticipantId(participant.getParticipantId());
        participantDto.setExperimentId(participant.getExperiment().getExperimentId());
        participantDto.setUser(userToDTO(participant.getLtiUserEntity()));
        participantDto.setConsent(participant.getConsent());
        participantDto.setDateGiven(participant.getDateGiven());
        participantDto.setDateRevoked(participant.getDateRevoked());
        participantDto.setSource(participant.getSource().name());
        participantDto.setDropped(participant.getDropped());

        if (participant.getGroup() != null) {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }

        participantDto.setStarted(hasParticipantSubmitted(participant, publishedExperimentAssignmentIds));

        return participantDto;
    }

    private UserDto userToDTO(LtiUserEntity user) {
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getUserId());
        userDto.setUserKey(user.getUserKey());
        userDto.setDisplayName(user.getDisplayName());

        return userDto;
    }

    @Override
    public Participant fromDto(ParticipantDto participantDto) throws DataServiceException {
        Participant participant = new Participant();
        Optional<Experiment> experiment = experimentRepository.findById(participantDto.getExperimentId());

        if (experiment.isEmpty()) {
            throw new DataServiceException("The experiment for the participant does not exist");
        }

        participant.setExperiment(experiment.get());

        try {
            Optional<LtiUserEntity> userEntity = ltiUserRepository.findById(participantDto.getUser().getUserId());

            if (userEntity.isEmpty()) {
                throw new DataServiceException("The user for the participant does not exist");
            }

            participant.setLtiUserEntity(userEntity.get());
        } catch (Exception e) {
            throw new DataServiceException("The user for the participant is not valid", e);
        }

        if (participantDto.getGroupId() != null && groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())) {
            participant.setGroup(groupRepository.getReferenceById(participantDto.getGroupId()));
        }

        participant.setParticipantId(participant.getParticipantId());
        participant.setConsent(participantDto.getConsent());
        participant.setDateGiven(participantDto.getDateGiven());
        participant.setDateRevoked(participantDto.getDateRevoked());
        participant.setDropped(participantDto.getDropped());
        participant.setSource(experiment.get().getParticipationType());

        return participant;
    }

    @Override
    public void saveAndFlush(Participant participantToChange) {
        participantRepository.saveAndFlush(participantToChange);
    }

    @Override
    @Transactional
    public List<Participant> refreshParticipants(long experimentId, List<Participant> currentParticipantList)
            throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        long startTime = System.currentTimeMillis();

        // We don't want to delete participants if they drop the course, so... we
        // will keep the all participants But we will need to mark them as
        // dropped if they are not in the course roster. So... a way to create a
        // hash table of all current participants and remove them as we find them
        // in the course roster. Any left over need to be marked as dropped.
        Map<String, Participant> participantsToBeDropped = new HashMap<>();

        for (Participant participant : currentParticipantList) {
            participantsToBeDropped.put(participant.getLtiUserEntity().getUserKey(), participant);
        }

        List<Participant> newParticipantList = new ArrayList<>(currentParticipantList);

        try {
            Optional<Experiment> experiment = experimentRepository.findById(experimentId);

            if (experiment.isEmpty()) {
                throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
            }

            LTIToken ltiToken = advantageMembershipService.getToken(experiment.get().getPlatformDeployment());
            CourseUsers courseUsers = advantageMembershipService.callMembershipService(ltiToken, experiment.get().getLtiContextEntity());

            for (CourseUser courseUser : courseUsers.getCourseUserList()) {
                if (courseUser.getRoles().contains(Roles.LEARNER) || courseUser.getRoles().contains(Roles.MEMBERSHIP_LEARNER)) {
                    Participant participant = participantsToBeDropped.remove(courseUser.getUserId());
                    if (participant != null) {
                        resetParticipantConsentIfExperimentNotStarted(experiment.get(), participant);

                        // If participant is marked as dropped, mark it as not dropped
                        if (BooleanUtils.isTrue(participant.getDropped())) {
                            participant.setDropped(false);
                            participantRepository.save(participant);
                        }
                    } else {
                        Participant newParticipant = createNewParticipant(courseUser, experiment.get());
                        newParticipantList.add(newParticipant);
                    }
                }
            }

            // Mark as dropped any participants not found in course roster
            for (Participant participantToDrop : participantsToBeDropped.values()) {
                participantToDrop.setDropped(true);
                participantRepository.save(participantToDrop);
            }
        } catch (ConnectionException | NoSuchElementException e) {
            throw new ParticipantNotUpdatedException(e.getMessage());
        }

        participantRepository.flush();

        log.debug("Refreshing participants for experiment {} took {}s", experimentId,
                (System.currentTimeMillis() - startTime) / 1000f);

        return newParticipantList;
    }

    /**
     * If experiment hasn't started and participation type has changed, reset the
     * participant's consent.
     *
     * @param experiment
     * @param participant
     */
    public void resetParticipantConsentIfExperimentNotStarted(Experiment experiment, Participant participant) {
        if (experiment.getStarted() == null && experiment.getParticipationType() != participant.getSource()) {
            switch (participant.getSource()) {
                case NOSET:
                    switch (experiment.getParticipationType()) {
                        case MANUAL:
                            participant.setConsent(null);
                            break;
                        case CONSENT:
                            participant.setConsent(false);
                            break;
                        case AUTO:
                            participant.setConsent(true);
                            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                            break;
                        default:
                    }
                    break;
                case AUTO:
                    switch (experiment.getParticipationType()) {
                        case MANUAL:
                            participant.setConsent(null);
                            participant.setDateGiven(null);
                            participant.setDateRevoked(null);
                            break;
                        case CONSENT:
                            participant.setConsent(false);
                            participant.setDateGiven(null);
                            participant.setDateRevoked(null);
                            break;
                        case AUTO:
                            break;
                        default:
                    }
                    break;
                case MANUAL:
                    switch (experiment.getParticipationType()) {
                        case MANUAL:
                            break;
                        case CONSENT:
                            participant.setConsent(false);
                            participant.setDateGiven(null);
                            participant.setDateRevoked(null);
                            break;
                        case AUTO:
                            participant.setConsent(true);
                            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                            participant.setDateRevoked(null);
                            break;
                        default:
                    }
                    break;
                case CONSENT:
                    switch (experiment.getParticipationType()) {
                        case MANUAL:
                            participant.setConsent(null);
                            participant.setDateGiven(null);
                            participant.setDateRevoked(null);
                            break;
                        case CONSENT:
                            break;
                        case AUTO:
                            participant.setConsent(true);
                            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                            participant.setDateRevoked(null);
                            break;
                        default:
                    }
                    break;
                default:
            }

            participant.setSource(experiment.getParticipationType());
            participantRepository.save(participant);
        }
    }

    private Participant createNewParticipant(CourseUser courseUser, Experiment experiment) {
        LtiUserEntity ltiUserEntity = ltiDataService.findByUserKeyAndPlatformDeployment(courseUser.getUserId(),
                experiment.getPlatformDeployment());

        if (ltiUserEntity == null) {
            LtiUserEntity newLtiUserEntity = new LtiUserEntity(courseUser.getUserId(), null,
                    experiment.getPlatformDeployment());
            newLtiUserEntity.setEmail(courseUser.getEmail());
            // TODO: We don't have a way here to get the userCanvasId except calling the API
            // or waiting for the user to access. BUT we just need this to send the grades
            // with the API...
            // so if the user never accessed... we can't send them until we use LTI.
            newLtiUserEntity.setDisplayName(courseUser.getName());
            // By default it adds a value in the constructor, but if we are generating it,
            // it means that the user has never logged in
            newLtiUserEntity.setLoginAt(null);
            ltiUserEntity = ltiDataService.saveLtiUserEntity(newLtiUserEntity);

        }

        LtiMembershipEntity ltiMembershipEntity = ltiDataService.findByUserAndContext(ltiUserEntity, experiment.getLtiContextEntity());

        if (ltiMembershipEntity == null) {
            ltiMembershipEntity = new LtiMembershipEntity(experiment.getLtiContextEntity(), ltiUserEntity,
                    LtiStrings.ROLE_STUDENT);
            ltiMembershipEntity = ltiDataService.saveLtiMembershipEntity(ltiMembershipEntity);
        }

        Participant newParticipant = new Participant();
        newParticipant.setExperiment(experiment);
        newParticipant.setLtiUserEntity(ltiUserEntity);
        newParticipant.setLtiMembershipEntity(ltiMembershipEntity);
        newParticipant.setDropped(false);
        newParticipant.setSource(experiment.getParticipationType());

        switch (experiment.getParticipationType()) {
            case MANUAL:
                newParticipant.setConsent(null);
                break;
            case CONSENT:
                newParticipant.setConsent(false);
                break;
            case AUTO:
                newParticipant.setConsent(true);
                newParticipant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                break;
            default:
        }

        newParticipant = participantRepository.save(newParticipant);

        return newParticipant;
    }

    @Override
    @Transactional
    public void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        List<Participant> currentParticipantList = findAllByExperimentId(experimentId);
        refreshParticipants(experimentId, currentParticipantList);
    }

    @Override
    @Transactional
    public void changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId, SecuredInfo securedInfo) {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experimentId, securedInfo.getCanvasCourseId(), experiment.getCreatedBy());

        for (Map.Entry<Participant, ParticipantDto> entry : map.entrySet()) {
            Participant participantToChange = entry.getKey();
            ParticipantDto participantDto = entry.getValue();

            // If they had consent, and now they don't have, we change the dateRevoked to now.
            // In any other case, we leave the date as it is. Ignoring any value in the PUT
            if (participantToChange.getConsent() != null
                    && (BooleanUtils.isTrue(participantToChange.getConsent()) || (BooleanUtils.isFalse(participantToChange.getConsent()) && participantToChange.getDateRevoked() == null))
                    && BooleanUtils.isNotTrue(participantDto.getConsent())) {
                participantToChange.setDateGiven(null);
                participantToChange.setDateRevoked(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setSource(ParticipationTypes.REVOKED);
            }

            // update non-consented to consented; reset revoked date; set source to experiment type
            if (BooleanUtils.isNotTrue(participantToChange.getConsent()) && BooleanUtils.isTrue(participantDto.getConsent())) {
                participantToChange.setDateGiven(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setDateRevoked(null);
                participantToChange.setSource(experiment.getParticipationType());
            }

            participantToChange.setConsent(participantDto.getConsent());

            // NOTE: we do this... but this will be updated in the next GET participants with the real data and dropped will be overwritten.
            if (participantDto.getDropped() != null) {
                participantToChange.setDropped(participantDto.getDropped());
            }

            // We don't allow changing the group (manually) once the experiment has started.
            if (!hasParticipantSubmitted(participantToChange, publishedExperimentAssignmentIds)) {
                if (participantDto.getGroupId() != null
                        && groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.getExperimentId(), participantDto.getGroupId())) {
                    participantToChange.setGroup(groupRepository.findByGroupId(participantDto.getGroupId()));
                } else {
                    participantToChange.setGroup(null);
                }
            }

            if (participantToChange.getSource() == null) {
                participantToChange.setSource(experiment.getParticipationType());
            }

            participantRepository.save(participantToChange);
        }
    }

    @Override
    @Transactional
    public boolean changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId) throws ParticipantAlreadyStartedException, ExperimentNotMatchingException {
        Participant participant = participantRepository.findByParticipantId(participantDto.getParticipantId());

        if (participant == null
                || !StringUtils.equals(participant.getLtiUserEntity().getUserKey(), securedInfo.getUserId())
                || securedInfo.getConsent() == null
                || BooleanUtils.isFalse(securedInfo.getConsent())) {
            return false;
        }

        if (BooleanUtils.isTrue(participant.getConsent()) && BooleanUtils.isFalse(participantDto.getConsent())) {
            // user is changing consent from true to false, set source = REVOKED
            participant.setSource(ParticipationTypes.REVOKED);
        }

        // Don't allow changing consent to true if participant has submitted a response and previously not consented
        if (hasParticipantSubmitted(participant, calculatedPublishedAssignmentIds(experimentId, securedInfo.getCanvasCourseId(), participant.getExperiment().getCreatedBy()))
                && BooleanUtils.isFalse(participant.getConsent())
                && BooleanUtils.isTrue(participantDto.getConsent())) {
            throw new ParticipantAlreadyStartedException("Participant has already started experiment, consent cannot be changed to given");
        }

        // We only edit the consent here.
        participantDto.setDropped(participant.getDropped());

        if (participant.getGroup() == null) {
            participantDto.setGroupId(null);
        } else {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }

        changeParticipant(Collections.singletonMap(participant, participantDto), experimentId, securedInfo);

        // update experiment as started
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            throw new ExperimentNotMatchingException(String.format("No experiment with ID: '%s' found.", experimentId));
        }

        if (!experiment.isStarted()) {
            experiment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
        }

        return true;
    }

    @Override
    public Participant findParticipant(List<Participant> participants, String userId) {
        return participants.stream()
            .filter(participant -> StringUtils.equals(participant.getLtiUserEntity().getUserKey(), userId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Has the participant submitted a response to an assignment?
     *
     * true if:
     *
     * 1. has at least created a submission (viewed) a multi-version assignment
     *
     * false if none of the above and:
     *
     * 1. has only accessed and/or submitted to a single-version assignment
     *
     * @param participant
     * @param securedInfo
     * @return
     */
    private boolean hasParticipantSubmitted(Participant participant, List<Long> publishedExperimentAssignmentIds) {
        // find only published assignment submissions
        List<Submission> publishedSubmissions = submissionRepository.findByParticipant_ParticipantId(participant.getParticipantId()).stream()
            .filter(submission -> publishedExperimentAssignmentIds.contains(submission.getAssessment().getTreatment().getAssignment().getAssignmentId()))
            .toList();

        return
            // participant has at least viewed a multi-version assignment; consider it submitted
            CollectionUtils.isNotEmpty(
                publishedSubmissions.stream()
                    .filter(publishedSubmission -> treatmentRepository.findByAssignment_AssignmentId(publishedSubmission.getAssessment().getTreatment().getAssignment().getAssignmentId()).size() > 1)
                    .toList()
            );
    }

    @Override
    public List<Long> calculatedPublishedAssignmentIds(long experimentId, String canvasCourseId, LtiUserEntity createdBy) {
        // find only published assignments
        return assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId).stream()
            .filter(
                assignment -> {
                    try {
                        return canvasAPIClient.listAssignment(createdBy, canvasCourseId, Long.parseLong(assignment.getLmsAssignmentId())).get().isPublished();
                    } catch (Exception e) {
                        return false;
                    }
                }
            )
            .map(Assignment::getAssignmentId)
            .toList();
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long participantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/participant/{participantId}")
                .buildAndExpand(experimentId, participantId).toUri());

        return headers;
    }

    @Override
    @Transactional
    public void setAllToNull(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, participants);

        for (Participant participant : participants) {
            participant.setConsent(null);
            participant.setDateGiven(null);
        }

        participantRepository.saveAll(participants);
    }

    @Override
    @Transactional
    public void setAllToTrue(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, participants);

        for (Participant participant : participants) {
            participant.setConsent(true);
            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
        }

        participantRepository.saveAll(participants);
    }

    @Override
    @Transactional
    public void setAllToFalse(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, participants);

        for (Participant participant : participants) {
            participant.setConsent(false);
            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
        }

        participantRepository.saveAll(participants);
    }

    @Override
    public void postConsentSubmission(Participant participant, SecuredInfo securedInfo) throws ConnectionException, DataServiceException {
        // need the assignment and the iss configuration
        PlatformDeployment platformDeployment = participant.getExperiment().getPlatformDeployment();
        Experiment experiment = participant.getExperiment();
        LTIToken ltiTokenScore = advantageAGSService.getToken("scores", platformDeployment);
        LTIToken ltiTokenResults = advantageAGSService.getToken("results", platformDeployment);
        // find the right id to pass based on the assignment
        LTIToken ltiToken = advantageAGSService.getToken("lineitems", experiment.getPlatformDeployment());
        // find the right id to pass based on the assignment
        LineItems lineItems = advantageAGSService.getLineItems(ltiToken, experiment.getLtiContextEntity());

        Optional<LineItem> lineItem = lineItems.getLineItemList().stream()
            .filter(li -> StringUtils.equals(li.getResourceLinkId(), participant.getExperiment().getConsentDocument().getResourceLinkId()))
            .findFirst();

        if (lineItem.isEmpty()) {
            // if we couldn't find lineitem, try to get the resource link id anew
            // (This is needed because the resourceLinkId for a consent assignment
            // wasn't accurately assigned previously. Eventually this can be removed.)
            lineItem = fixConsentAssignmentResourceLinkId(securedInfo, experiment, lineItems, lineItem);
        }

        if (lineItem.isEmpty()) {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }

        Score score = new Score();
        score.setUserId(participant.getLtiUserEntity().getUserKey());
        // Score the consent submission as 100% and let the platform scale the grade to the max number of points.
        score.setScoreGiven("1.0");
        score.setScoreMaximum("1.0");
        score.setActivityProgress("Completed");
        score.setGradingProgress("FullyGraded");

        Date date = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String strDate = dt.format(date);
        score.setTimestamp(strDate);
        advantageAGSService.postScore(ltiTokenScore, ltiTokenResults, experiment.getLtiContextEntity(), lineItem.get().getId(), score);
    }

    /**
     * This is needed because the resourceLinkId for a consent assignment wasn't
     * accurately assigned previous to the fix in TCOTA-430. Eventually this can be
     * removed.
     *
     * @param securedInfo
     * @param platformDeployment
     * @param experiment
     * @param lineItems
     * @param lineItem
     * @return
     * @throws DataServiceException
     */
    private Optional<LineItem> fixConsentAssignmentResourceLinkId(SecuredInfo securedInfo, Experiment experiment, LineItems lineItems, Optional<LineItem> lineItem)
            throws DataServiceException {
        long assignmentId = Long.parseLong(experiment.getConsentDocument().getLmsAssignmentId());

        try {
            log.warn(
                    "Could not find line item for experiment {} consent assignment. Going to use "
                            + "Canvas API to try to figure out the right resourceLinkId. This is only "
                            + "for an older issue with setting the resourceLinkId correctly so this "
                            + "should NEVER happen with new experiments.",
                    experiment.getExperimentId());
            LtiUserEntity instructorUser = experiment.getCreatedBy();
            Optional<AssignmentExtended> consentAssignment = canvasAPIClient.listAssignment(instructorUser, securedInfo.getCanvasCourseId(), assignmentId);

            if (consentAssignment.isPresent()) {
                String jwtTokenAssignment = consentAssignment.get().getSecureParams();
                String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).get("lti_assignment_id").toString();
                lineItem = lineItems.getLineItemList().stream()
                    .filter(li -> li.getResourceLinkId()
                    .equals(resourceLinkId))
                    .findFirst();

                // If we now have a lineitem, save it with the consent document
                if (lineItem.isPresent()) {
                    log.info("Updating the resourceLinkId to {} for the consent assignment of experiment {}", resourceLinkId, experiment.getExperimentId());
                    experiment.getConsentDocument().setResourceLinkId(resourceLinkId);
                    consentDocumentRepository.save(experiment.getConsentDocument());
                }
            }
        } catch (CanvasApiException e) {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }

        return lineItem;
    }

    @Override
    @Transactional
    public Participant handleExperimentParticipant(Experiment experiment, SecuredInfo securedInfo)
            throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException,
                    ExperimentNotMatchingException {
        Participant participant = participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experiment.getExperimentId(), securedInfo.getUserId());

        // if participant record doesn't exist or if consenting participant
        // isn't assigned to a group or if participant record does exist but it
        // is marked as dropped, refresh the participant list
        if (participant == null
                || (BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null)
                || BooleanUtils.isTrue(participant.getDropped())) {
            participant = findParticipant(refreshParticipants(experiment.getExperimentId(), experiment.getParticipants()), securedInfo.getUserId());
        }

        if (participant == null) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }

        // 1. Check if the student has the consent signed. If not, set it as no participant
        handleInitialConsent(experiment, participant, securedInfo);

        // 2. Check if the student is in a group (and if not assign it to the right one if consent is true)
        if (BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null) {
            if (DistributionTypes.CUSTOM.equals(experiment.getDistributionType())) {
                for (Condition condition : experiment.getConditions()) {
                    if (BooleanUtils.isTrue(condition.getDefaultCondition())) {
                        participant.setGroup(groupParticipantService.getUniqueGroupByConditionId(experiment.getExperimentId(), securedInfo.getCanvasAssignmentId(), condition.getConditionId()));
                        break;
                    }
                }
            } else { // We assign it to the more unbalanced group (if consent is true)
                participant.setGroup(groupParticipantService.nextGroup(experiment));
            }
        }

        return participantRepository.save(participant);
    }

    /**
     * Sets participant consent if any of the following are true:
     *
     * 1. Particpation type is "auto"
     * 2. Experiment is not a single condition
     * 3. Participant has not submitted to an assignment (per conditions in hasParticipantSubmitted method)
     *
     * @param experiment
     * @param participant
     */
    private void handleInitialConsent(Experiment experiment, Participant participant, SecuredInfo securedInfo) {
        List<Long> publishedExperimentAssignmentIds = calculatedPublishedAssignmentIds(experiment.getExperimentId(), securedInfo.getCanvasCourseId(), experiment.getCreatedBy());
        if (participant.getConsent() == null || (!participant.getConsent() && participant.getDateRevoked() == null)) {
            if (ParticipationTypes.AUTO.equals(experiment.getParticipationType())) {
                participant.setConsent(true);
                participant.setDateGiven(Timestamp.from(Instant.now()));

                return;
            }

            if (!hasParticipantSubmitted(participant, publishedExperimentAssignmentIds)) {
                // participant has no submissions
                return;
            }

            participant.setConsent(false);
            participant.setDateRevoked(Timestamp.from(Instant.now()));
        }
    }

}
