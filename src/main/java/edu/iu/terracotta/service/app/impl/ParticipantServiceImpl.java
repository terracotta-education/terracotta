package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiContextEntity;
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
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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

        participant.setParticipantId(participant.getParticipantId());
        participant.setConsent(participantDto.getConsent());
        participant.setDateGiven(participantDto.getDateGiven());
        participant.setDateRevoked(participantDto.getDateRevoked());
        participant.setDropped(participantDto.getDropped());
        //Default value will be the defined in the Experiment ParticipationType
        //TODO, not here, but we need to write code to manage changes in the experiment participation type. (In the exepriment/PUT)
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
        allRepositories.participantRepository.deleteById(id);
    }

    @Override
    public List<Participant> refreshParticipants(long experimentId, SecurityInfo securityInfo, List<Participant> currentParticipantList) throws ParticipantNotUpdatedException {

        List<Participant> newParticipantList = new ArrayList<>();
        //We don't want to delete participants if they drop the course, so... we will keep the all participants
        //But we will need to mark them as dropped if they are not in the next list. So... a way to do it is to mark
        //all as dropped and then refresh the list with the new ones.
        for (Participant participant:currentParticipantList){
            participant.setDropped(true);
        }
        newParticipantList.addAll(currentParticipantList);
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
                    boolean alredyInList = false;
                    for (Participant participant : currentParticipantList) {
                        if (participant.getLtiMembershipEntity().getUser().getUserKey().equals(ltiUserEntity.getUserKey())) {
                            alredyInList = true;
                            participant.setDropped(false);
                        }
                    }
                    if (!alredyInList) {
                        Participant newParticipant = new Participant();
                        newParticipant.setExperiment(experiment);
                        newParticipant.setLtiUserEntity(ltiUserEntity);
                        newParticipant.setLtiMembershipEntity(ltiMembershipEntity);
                        newParticipant.setDropped(false);
                        newParticipant.setSource(experiment.getParticipationType());
                        switch (experiment.getParticipationType()){
                            case MANUAL:
                                newParticipant.setConsent(false);
                                newParticipant.setDateGiven(new Timestamp(System.currentTimeMillis()));
                                break;
                            case CONSENT:
                                newParticipant.setConsent(false);
                                //We don't set date here, because the date will be used to know if the students
                                //ever checked a value. so a value with date means that the student answered the consent form assignment.
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
    public void prepareParticipation(Long experimentId, SecurityInfo securityInfo) throws ParticipantNotUpdatedException {

        List<Participant> currentParticipantList =
                findAllByExperimentId(experimentId);
        refreshParticipants(experimentId,securityInfo,currentParticipantList);
    }
}
