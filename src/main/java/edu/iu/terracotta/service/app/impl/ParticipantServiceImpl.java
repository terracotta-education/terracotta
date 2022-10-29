package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
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
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ParticipantServiceImpl implements ParticipantService {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantServiceImpl.class);

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private AdvantageMembershipService advantageMembershipService;

    @Autowired
    private LTIDataService ltiDataService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private AdvantageAGSService advantageAGSService;

    @Autowired
    private CanvasAPIClient canvasAPIClient;

    @Autowired
    private APIJWTService apijwtService;

    @Override
    public List<Participant> findAllByExperimentId(long experimentId) {
        return allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<ParticipantDto> getParticipants(List<Participant> participants, long experimentId, String userId, boolean student) {
        List<ParticipantDto> participantDtoList = new ArrayList<>();
        if (!student) {
            for (Participant participant : participants) {
                participantDtoList.add(toDto(participant));
            }
            return participantDtoList;
        }
        try {
            participantDtoList.add(toDto(allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId)));
        } catch (NullPointerException ex) {
            //A null pointer means that there is no participant for this experiment with that userId. We will return an empty list.
            return participantDtoList;
        }
        return participantDtoList;
    }

    @Override
    public Participant getParticipant(long participantId, long experimentId, String userId, boolean student) throws InvalidUserException {
        if (!student) {
            return allRepositories.participantRepository.findByParticipantId(participantId);
        }

        Participant participant = allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, userId);
        if (participant.getParticipantId().equals(participantId)) {
            return allRepositories.participantRepository.findByParticipantId(participantId);
        } else {
            throw new InvalidUserException("Error 146: Students are not authorized to view other participants.");
        }
    }

    @Override
    public ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId) throws IdInPostException, DataServiceException {
        if (participantDto.getParticipantId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        Participant participant;
        participantDto.setExperimentId(experimentId);
        try {
            participant = fromDto(participantDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create the participant:" + e.getMessage());
        }
        return toDto(save(participant));
    }

    @Override
    public ParticipantDto toDto(Participant participant) {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setParticipantId(participant.getParticipantId());
        participantDto.setExperimentId(participant.getExperiment().getExperimentId());
        participantDto.setUser(userToDTO(participant.getLtiMembershipEntity().getUser()));
        participantDto.setConsent(participant.getConsent());
        participantDto.setDateGiven(participant.getDateGiven());
        participantDto.setDateRevoked(participant.getDateRevoked());
        participantDto.setSource(participant.getSource().name());
        participantDto.setDropped(participant.getDropped());
        if (participant.getGroup() != null) {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }
        participantDto.setStarted(hasStarted(participant));
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
    public Optional<Participant> findById(Long id) {
        return allRepositories.participantRepository.findById(id);
    }

    @Override
    public Optional<Participant> findByParticipantIdAndExperimentId(Long participantId, Long experimentId) {
        return allRepositories.participantRepository.findByParticipantIdAndExperiment_ExperimentId(participantId, experimentId);
    }

    @Override
    public Participant fromDto(ParticipantDto participantDto) throws DataServiceException {
        Participant participant = new Participant();
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(participantDto.getExperimentId());
        if (experiment.isPresent()) {
            participant.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the participant does not exist");
        }
        try {
            Optional<LtiUserEntity> userEntity = allRepositories.users.findById(participantDto.getUser().getUserId());
            if (userEntity.isPresent()) {
                participant.setLtiUserEntity(userEntity.get());
            } else {
                throw new DataServiceException("The user for the participant does not exist");
            }
        } catch (Exception e) {
            throw new DataServiceException("The user for the participant is not valid");
        }
        if (participantDto.getGroupId() != null && allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())) {
            participant.setGroup(allRepositories.groupRepository.getOne(participantDto.getGroupId()));
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
        allRepositories.participantRepository.saveAndFlush(participantToChange);
    }

    @Override
    public Participant save(Participant participant) {
        return allRepositories.participantRepository.save(participant);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.participantRepository.deleteByParticipantId(id);
    }

    @Override
    @Transactional
    public List<Participant> refreshParticipants(long experimentId, SecuredInfo securedInfo, List<Participant> currentParticipantList) throws ParticipantNotUpdatedException {

        //We don't want to delete participants if they drop the course, so... we will keep the all participants
        //But we will need to mark them as dropped if they are not in the next list. So... a way to do it is to mark
        //all as dropped and then refresh the list with the new ones.
        for (Participant participant : currentParticipantList) {
            participant.setDropped(true);
        }
        List<Participant> newParticipantList = new ArrayList<>(currentParticipantList);
        try {
            Experiment experiment = allRepositories.experimentRepository.findById(experimentId).get();
            LTIToken ltiToken = advantageMembershipService.getToken(experiment.getPlatformDeployment());
            CourseUsers courseUsers = advantageMembershipService.callMembershipService(ltiToken, experiment.getLtiContextEntity());

            for (CourseUser courseUser : courseUsers.getCourseUserList()) {
                if (courseUser.getRoles().contains(Roles.LEARNER) || courseUser.getRoles().contains(Roles.MEMBERSHIP_LEARNER)) {
                    LtiUserEntity ltiUserEntity = ltiDataService.findByUserKeyAndPlatformDeployment(courseUser.getUserId(), experiment.getPlatformDeployment());

                    if (ltiUserEntity == null) {
                        LtiUserEntity newLtiUserEntity = new LtiUserEntity(courseUser.getUserId(), null, experiment.getPlatformDeployment());
                        newLtiUserEntity.setEmail(courseUser.getEmail());
                        //TODO: We don't have a way here to get the userCanvasId except calling the API
                        // or waiting for the user to access. BUT we just need this to send the grades with the API...
                        // so if the user never accessed... we can't send them until we use LTI.
                        newLtiUserEntity.setDisplayName(courseUser.getName());
                        //By default it adds a value in the constructor, but if we are generating it, it means that the user has never logged in
                        newLtiUserEntity.setLoginAt(null);
                        ltiUserEntity = ltiDataService.saveLtiUserEntity(newLtiUserEntity);


                    }
                    LtiMembershipEntity ltiMembershipEntity = ltiDataService.findByUserAndContext(ltiUserEntity, experiment.getLtiContextEntity());
                    if (ltiMembershipEntity == null) {
                        //TODO: Note, role 0 is student, surely we want to use a constant for that.
                        ltiMembershipEntity = new LtiMembershipEntity(experiment.getLtiContextEntity(), ltiUserEntity, 0);
                        ltiMembershipEntity = ltiDataService.saveLtiMembershipEntity(ltiMembershipEntity);
                    }
                    boolean alreadyInList = false;
                    for (Participant participant : currentParticipantList) {
                        if (participant.getLtiMembershipEntity().getUser().getUserKey().equals(ltiUserEntity.getUserKey())) {
                            alreadyInList = true;
                            if (experiment.getStarted() == null) {
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
                                        participant.setSource(experiment.getParticipationType());
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
                            }
                            participant.setDropped(false);
                            allRepositories.participantRepository.save(participant);
                        }
                    }
                    if (!alreadyInList) {
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
                        newParticipant = allRepositories.participantRepository.save(newParticipant);
                        newParticipantList.add(newParticipant);
                    }
                }
            }
        } catch (ConnectionException | NoSuchElementException e) {
            throw new ParticipantNotUpdatedException(e.getMessage());
        }
        allRepositories.participantRepository.flush();
        return newParticipantList;
    }

    @Override
    public boolean participantBelongsToExperiment(Long experimentId, Long participantId) {
        return allRepositories.participantRepository.existsByExperiment_ExperimentIdAndParticipantId(experimentId, participantId);
    }

    @Override
    @Transactional
    public void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException {

        List<Participant> currentParticipantList =
                findAllByExperimentId(experimentId);
        refreshParticipants(experimentId, securedInfo, currentParticipantList);
    }

    @Override
    @Transactional
    public void changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId) {
        for (Map.Entry<Participant, ParticipantDto> entry : map.entrySet()) {
            Participant participantToChange = entry.getKey();
            ParticipantDto participantDto = entry.getValue();
            Experiment experiment = experimentService.getExperiment(experimentId);
            if (experiment.getParticipationType().equals(ParticipationTypes.CONSENT)) {
                if ((experiment.getStarted() == null)
                        && ((participantToChange.getConsent() == null || (!participantToChange.getConsent() && participantToChange.getDateRevoked() == null))
                        && participantDto.getConsent() != null)) {
                    experiment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
                    experimentService.save(experiment);
                }
            }
            //If they had consent, and now they don't have, we change the dateRevoked to now.
            //In any other case, we leave the date as it is. Ignoring any value in the PUT
            if (participantToChange.getConsent() != null &&
                    (participantToChange.getConsent() || (!participantToChange.getConsent() && participantToChange.getDateRevoked() == null)) &&
                    (participantDto.getConsent() == null || !participantDto.getConsent())) {
                participantToChange.setDateRevoked(Timestamp.valueOf(LocalDateTime.now()));
            }
            if ((participantToChange.getConsent() == null || !participantToChange.getConsent()) &&
                    (participantDto.getConsent() != null && participantDto.getConsent())) {
                participantToChange.setDateGiven(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setDateRevoked(null);
            }
            participantToChange.setConsent((participantDto.getConsent()));

            //NOTE: we do this... but this will be updated in the next GET participants with the real data and dropped will be overwritten.
            if (participantDto.getDropped() != null) {
                participantToChange.setDropped(participantDto.getDropped());
            }
            if (!hasStarted(participantToChange)) { //We don't allow changing the group (manually) once the experiment has started.
                if (participantDto.getGroupId() != null && groupService.existsByExperiment_ExperimentIdAndGroupId(experiment.getExperimentId(), participantDto.getGroupId())) {
                    participantToChange.setGroup(groupService.getGroup(participantDto.getGroupId()));
                } else {
                    participantToChange.setGroup(null);
                }
            }
            participantToChange.setSource(experiment.getParticipationType());

            save(participantToChange);
        }
    }

    @Override
    @Transactional
    public boolean changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId) throws ParticipantAlreadyStartedException {

        Participant participant = allRepositories.participantRepository.findByParticipantId(participantDto.getParticipantId());
        if (participant == null
                || !participant.getLtiUserEntity().getUserKey().equals(securedInfo.getUserId())
                || securedInfo.getConsent() == null || !securedInfo.getConsent()) {
            return false;
        }

        // Don't allow changing consent to consent=true if started and not consenting
        if (hasStarted(participant) && participant.getConsent() != null && !participant.getConsent()
                && participantDto.getConsent() != null && participantDto.getConsent()) {
            throw new ParticipantAlreadyStartedException("Participant has already started experiment, consent cannot be changed to given");
        }
        //We only edit the consent here.
        participantDto.setDropped(participant.getDropped());
        if (participant.getGroup() == null) {
            participantDto.setGroupId(null);
        } else {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }
        Map<Participant, ParticipantDto> map = new HashMap<>();
        map.put(participant, participantDto);
        changeParticipant(map, experimentId);

        return true;


    }


    @Override
    public Participant findParticipant(List<Participant> participants, String userId) {
        for (Participant participant : participants) {
            if (participant.getLtiUserEntity().getUserKey().equals(userId)) {
                return participant;
            }
        }
        return null;
    }

    private boolean hasStarted(Participant participant) {
        //to know if he has started we need to find at least one submission.
        return !allRepositories.submissionRepository.findByParticipant_ParticipantId(participant.getParticipantId()).isEmpty();
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
    public void setAllToNull(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException {
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, securedInfo, participants);
        for (Participant participant : participants) {
            participant.setConsent(null);
            participant.setDateGiven(null);
        }
        allRepositories.participantRepository.saveAll(participants);
    }

    @Override
    @Transactional
    public void setAllToTrue(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException {
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, securedInfo, participants);
        for (Participant participant : participants) {
            participant.setConsent(true);
            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
        }
        allRepositories.participantRepository.saveAll(participants);
    }

    @Override
    @Transactional
    public void setAllToFalse(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException {
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        refreshParticipants(experimentId, securedInfo, participants);
        for (Participant participant : participants) {
            participant.setConsent(false);
            participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
        }
        allRepositories.participantRepository.saveAll(participants);

    }

    @Override
    public void postConsentSubmission(Participant participant, SecuredInfo securedInfo) throws ConnectionException, DataServiceException {
        //We need, the assignment, and the iss configuration...
        PlatformDeployment platformDeployment = participant.getExperiment().getPlatformDeployment();
        Experiment experiment = participant.getExperiment();
        LTIToken ltiTokenScore = advantageAGSService.getToken("scores", platformDeployment);
        LTIToken ltiTokenResults = advantageAGSService.getToken("results", platformDeployment);
        //find the right id to pass based on the assignment
        LTIToken ltiToken = advantageAGSService.getToken("lineitems", experiment.getPlatformDeployment());
        //find the right id to pass based on the assignment
        LineItems lineItems = advantageAGSService.getLineItems(ltiToken, experiment.getLtiContextEntity());

        Optional<LineItem> lineItem = lineItems.getLineItemList().stream().filter(li -> li.getResourceLinkId()
                .equals(participant.getExperiment().getConsentDocument().getResourceLinkId())).findFirst();

        if (!lineItem.isPresent()) {
            // if we couldn't find lineitem, try to get the resource link id anew
            // (This is needed because the resourceLinkId for a consent assignment
            // wasn't accurately assigned previously. Eventually this can be removed.)
            lineItem = fixConsentAssignmentResourceLinkId(securedInfo, platformDeployment, experiment, lineItems,
                    lineItem);
        }

        if (lineItem.isPresent()) {
            Score score = new Score();
            score.setUserId(participant.getLtiUserEntity().getUserKey());
            // Score the consent submission as 100% and let the platform scale
            // the grade to the max number of points.
            score.setScoreGiven("1.0");
            score.setScoreMaximum("1.0");
            score.setActivityProgress("Completed");
            score.setGradingProgress("FullyGraded");

            Date date = new Date();
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String strDate = dt.format(date);
            score.setTimestamp(strDate);
            advantageAGSService.postScore(ltiTokenScore, ltiTokenResults,
                    experiment.getLtiContextEntity(), lineItem.get().getId(), score);
        } else {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }
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
    private Optional<LineItem> fixConsentAssignmentResourceLinkId(SecuredInfo securedInfo,
            PlatformDeployment platformDeployment,
            Experiment experiment, LineItems lineItems, Optional<LineItem> lineItem) throws DataServiceException {
        int assignmentId = Integer.parseInt(experiment.getConsentDocument().getLmsAssignmentId());
        try {
            logger.warn(
                    "Could not find line item for experiment {} consent assignment. Going to use "
                            + "Canvas API to try to figure out the right resourceLinkId. This is only "
                            + "for an older issue with setting the resourceLinkId correctly so this "
                            + "should NEVER happen with new experiments.",
                    experiment.getExperimentId());
            LtiUserEntity instructorUser = experiment.getCreatedBy();
            Optional<AssignmentExtended> consentAssignment = canvasAPIClient
                    .listAssignment(instructorUser, securedInfo.getCanvasCourseId(), assignmentId);
            if (consentAssignment.isPresent()) {
                String jwtTokenAssignment = consentAssignment.get().getSecureParams();
                String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).getBody()
                        .get("lti_assignment_id").toString();
                lineItem = lineItems.getLineItemList().stream().filter(li -> li.getResourceLinkId()
                        .equals(resourceLinkId)).findFirst();
                // If we now have a lineitem, save it with the consent document
                if (lineItem.isPresent()) {
                    logger.info("Updating the resourceLinkId to {} for the consent assignment of experiment {}",
                            resourceLinkId, experiment.getExperimentId());
                    experiment.getConsentDocument().setResourceLinkId(resourceLinkId);
                    experimentService.saveConsentDocument(experiment.getConsentDocument());
                }
            }
        } catch (CanvasApiException e) {
            throw new DataServiceException("Error 136: The assignment is not linked to any Canvas assignment");
        }
        return lineItem;
    }

    @Override
    @Transactional
    public Participant handleExperimentParticipant(Experiment experiment, SecuredInfo securedInfo) throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        List<Participant> participants = refreshParticipants(experiment.getExperimentId(), securedInfo, experiment.getParticipants());
        Participant participant = findParticipant(participants, securedInfo.getUserId());

        if (participant == null) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }

        // 1. Check if the student has the consent signed. If not, set it as no participant
        handleConsent(experiment, participant);

        // 2. Check if the student is in a group (and if not assign it to the right one if consent is true)
        if (BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null) {
            if (DistributionTypes.CUSTOM.equals(experiment.getDistributionType())) {
                for (Condition condition : experiment.getConditions()) {
                    if (BooleanUtils.isTrue(condition.getDefaultCondition())) {
                        participant.setGroup(groupService.getUniqueGroupByConditionId(experiment.getExperimentId(), securedInfo.getCanvasAssignmentId(), condition.getConditionId()));
                        break;
                    }
                }
            } else { // We assign it to the more unbalanced group (if consent is true)
                participant.setGroup(groupService.nextGroup(experiment));
            }
        }

        return save(participant);
    }

    private void handleConsent(Experiment experiment, Participant participant) {
        if (participant.getConsent() == null || (!participant.getConsent() && participant.getDateRevoked() == null)) {
            if (experiment.getParticipationType().equals(ParticipationTypes.AUTO)) {
                participant.setConsent(true);
                participant.setDateGiven(new Timestamp(System.currentTimeMillis()));
            } else {
                participant.setConsent(false);
                participant.setDateRevoked(new Timestamp(System.currentTimeMillis()));

            }
        }
    }

}
