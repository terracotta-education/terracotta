package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.dto.UserDto;
import edu.iu.terracotta.repository.AllRepositories;
import liquibase.pro.packaged.E;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ParticipantService {

    @Autowired
    AllRepositories allRepositories;


    public List<Participant> findAllByExperimentId(long experimentId) {
        return allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
    }

    public ParticipantDto toDto(Participant participant) {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setParticipantId(participant.getParticipantId());
        participantDto.setExperimentId(participant.getExperiment().getExperimentId());
        participantDto.setUser(userToDTO(participant.getLtiUserEntity()));
        return participantDto;
    }

    private UserDto userToDTO (LtiUserEntity user){
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getUserId());
        userDto.setUserKey(user.getUserKey());
        userDto.setDisplayName(user.getDisplayName());
        return userDto;
    }

    public Optional<Participant> findById(Long id) {
        return allRepositories.participantRepository.findById(id);
    }

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
        return participant;
    }

    public void saveAndFlush(Participant participantToChange) {
        allRepositories.participantRepository.saveAndFlush(participantToChange);
    }

    public Participant save(Participant participant) {
        return allRepositories.participantRepository.save(participant);
    }

    public void deleteById(Long id) {
        allRepositories.participantRepository.deleteById(id);
    }
}
