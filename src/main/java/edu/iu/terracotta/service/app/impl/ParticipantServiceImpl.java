package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.dto.UserDto;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ParticipantServiceImpl implements ParticipantService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AdvantageMembershipService advantageMembershipService;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    GroupService groupService;

    @Autowired
    ExperimentService experimentService;


    @Override
    public List<Participant> findAllByExperimentId(long experimentId) {
        return allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
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
        if (participant.getGroup()!=null) {
            participantDto.setGroupId(participant.getGroup().getGroupId());
        }
        return participantDto;
    }

    private UserDto userToDTO (LtiUserEntity user){
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
    public Optional<Participant> findByParticipantIdAndExperimentId(Long participantId, Long experimentId){
        return allRepositories.participantRepository.findByParticipantIdAndExperiment_ExperimentId(participantId, experimentId);
    }

    @Override
    public Participant fromDto(ParticipantDto participantDto) throws DataServiceException {
        Participant participant = new Participant();
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(participantDto.getExperimentId());
        if (experiment.isPresent()){
            participant.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the participant does not exist");
        }
        try {
        Optional<LtiUserEntity> userEntity = allRepositories.users.findById(participantDto.getUser().getUserId());
            if (userEntity.isPresent()){
                participant.setLtiUserEntity(userEntity.get());
            } else {
                throw new DataServiceException("The user for the participant does not exist");
            }
        } catch (Exception e){
            throw new DataServiceException("The user for the participant is not valid");
        }
        if (participantDto.getGroupId()!=null && allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())){
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
    public List<Participant> refreshParticipants(long experimentId, SecuredInfo securedInfo, List<Participant> currentParticipantList) throws ParticipantNotUpdatedException {

        //We don't want to delete participants if they drop the course, so... we will keep the all participants
        //But we will need to mark them as dropped if they are not in the next list. So... a way to do it is to mark
        //all as dropped and then refresh the list with the new ones.
        for (Participant participant:currentParticipantList){
            participant.setDropped(true);
        }
        List<Participant> newParticipantList = new ArrayList<>(currentParticipantList);
        try {
            Experiment experiment =  allRepositories.experimentRepository.findById(experimentId).get();
            LTIToken ltiToken = advantageMembershipService.getToken(experiment.getPlatformDeployment());
            CourseUsers courseUsers = advantageMembershipService.callMembershipService(ltiToken, experiment.getLtiContextEntity());

            for (CourseUser courseUser:courseUsers.getCourseUserList()){
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
                            participant.setDropped(false);
                        }
                    }
                    if (!alreadyInList) {
                        Participant newParticipant = new Participant();
                        newParticipant.setExperiment(experiment);
                        newParticipant.setLtiUserEntity(ltiUserEntity);
                        newParticipant.setLtiMembershipEntity(ltiMembershipEntity);
                        newParticipant.setDropped(false);
                        newParticipant.setSource(experiment.getParticipationType());
                        switch (experiment.getParticipationType()){
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
        return newParticipantList;
    }

    @Override
    public boolean participantBelongsToExperiment(Long experimentId, Long participantId) {
        return allRepositories.participantRepository.existsByExperiment_ExperimentIdAndParticipantId(experimentId,participantId);
    }

    @Override
    public void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException {

        List<Participant> currentParticipantList =
                findAllByExperimentId(experimentId);
        refreshParticipants(experimentId, securedInfo,currentParticipantList);
    }

    @Override
    public void changeParticipant(Participant participantToChange, ParticipantDto participantDto, Long experimentId){
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        if (experiment.get().getParticipationType().equals(ParticipationTypes.CONSENT)){
            if ((experiment.get().getStarted()==null) && (participantToChange.getConsent()==null && participantDto.getConsent()!=null)){
                experiment.get().setStarted(Timestamp.valueOf(LocalDateTime.now()));
                experimentService.save(experiment.get());
            }
        }
        //If they had consent, and now they don't have, we change the dateRevoked to now.
        //In any other case, we leave the date as it is. Ignoring any value in the PUT
        if (participantToChange.getConsent() !=null && participantToChange.getConsent() &&
                (participantDto.getConsent()==null || !participantDto.getConsent())) {
            participantToChange.setDateRevoked(Timestamp.valueOf(LocalDateTime.now()));
        }
        if ((participantToChange.getConsent()==null || !participantToChange.getConsent()) &&
                (participantDto.getConsent()!=null && participantDto.getConsent())){
            participantToChange.setDateGiven(Timestamp.valueOf(LocalDateTime.now()));
        }
        participantToChange.setConsent((participantDto.getConsent()));

        //NOTE: we do this... but this will be updated in the next GET participants with the real data and dropped will be overwritten.
        if (participantDto.getDropped()!=null) {
            participantToChange.setDropped(participantDto.getDropped());
        }
        if (participantDto.getGroupId()!=null && groupService.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())){
            participantToChange.setGroup(groupService.findById(participantDto.getGroupId()).get());
        } else {
            participantToChange.setGroup(null);
        }
    }

    @Override
    public Participant findParticipant(List<Participant> participants, String userId){
        for (Participant participant:participants){
            if (participant.getLtiUserEntity().getUserKey().equals(userId)) {
                return participant;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void saveAllParticipants(List<Participant> participantList) { allRepositories.participantRepository.saveAll(participantList); }
}
