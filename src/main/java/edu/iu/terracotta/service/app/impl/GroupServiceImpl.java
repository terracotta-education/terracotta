package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GroupServiceImpl implements GroupService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    ParticipantService participantService;

    @Autowired
    ExperimentService experimentService;

    @Override
    public List<Group> findAllByExperimentId(long experimentId) {
        return allRepositories.groupRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public Optional<Group> findOneByGroupId(long groupId) {
        return allRepositories.groupRepository.findByGroupId(groupId);
    }

    @Override
    public GroupDto toDto(Group group) {

        GroupDto groupDto = new GroupDto();
        groupDto.setGroupId(group.getGroupId());
        groupDto.setExperimentId(group.getExperiment().getExperimentId());
        groupDto.setName(group.getName());
        List<Participant> participantList = allRepositories.participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(groupDto.getExperimentId(), group.getGroupId());
        List<ParticipantDto> participantDtoList = new ArrayList<>();
        for (Participant participant:participantList){
            participantDtoList.add(participantService.toDto(participant));
        }
        groupDto.setParticipants(participantDtoList);
        return groupDto;
    }

    @Override
    public Group fromDto(GroupDto groupDto) throws DataServiceException {

        Group group = new Group();
        //test if nulls behave correctly?
        group.setGroupId(groupDto.getGroupId());
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(groupDto.getExperimentId());
        if(experiment.isPresent()) {
            group.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        group.setName(groupDto.getName());
        return group;
    }

    @Override
    public Group save(Group group) { return allRepositories.groupRepository.save(group); }

    @Override
    public ExposureGroupCondition saveExposureGroupCondition(ExposureGroupCondition exposureGroupCondition) { return allRepositories.exposureGroupConditionRepository.save(exposureGroupCondition); }

    @Override
    public Optional<Group> findById(Long id) { return allRepositories.groupRepository.findById(id); }

    @Override
    public void saveAndFlush(Group groupToChange) { allRepositories.groupRepository.saveAndFlush(groupToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.groupRepository.deleteById(id); }

    @Override
    public boolean groupBelongsToExperiment(Long experimentId, Long groupId) {
        return allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId,groupId);
    }


    //TO DO!  ASSIGN STUDENTS TO GROUPS WILL HAPPEN IN THE FIRST LAUNCH OF THE STUDENT EXCEPT IF MANUAL

    @Override
    @Transactional
    public void createAndAssignGroupsToConditionsAndExposures(Long experimentId, SecurityInfo securityInfo, boolean isCustom) throws DataServiceException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);
        if(experiment.isPresent()) {
            int numberOfGroups = experiment.get().getConditions().size();
            List<Group> groups = allRepositories.groupRepository.findByExperiment_ExperimentId(experimentId);
            if (groups.isEmpty()){
                //We create the groups but we don't assign people to them
                groups = createGroups(numberOfGroups, experiment.get());
            } else {
                if (groups.size() != experiment.get().getConditions().size()){
                    if (experimentService.experimentStarted(experiment.get())){
                        //This should never happen, but... just in case
                        throw new DataServiceException("The experiment has started but there is an error with the group amount");
                    } else {
                        //RESET THE PARTICIPANT GROUPS
                        for (Participant participant:experiment.get().getParticipants()){
                            participant.setGroup(null);
                            participantService.save(participant);
                        }
                        //DELETE THE GROUPS
                        allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                        allRepositories.groupRepository.deleteByExperiment_ExperimentId(experimentId);
                        //CREATE THEM AGAIN
                        groups = createGroups(numberOfGroups, experiment.get());
                    }
                }
            }
            //We assign the groups to the conditions and exposures
            List<ExposureGroupCondition> exposureGroupConditionList = allRepositories.exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experimentId);
            if (exposureGroupConditionList.isEmpty()) {
                assignGroups(groups, experiment.get());
            } else {
                if (exposureGroupConditionList.size()!=experiment.get().getConditions().size()*experiment.get().getExposures().size()) {
                    if (experimentService.experimentStarted(experiment.get())) {
                        throw new DataServiceException("The experiment has started but there is an error with the group/exposure/condition associations amount");
                    } else {
                        allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                        assignGroups(groups, experiment.get());
                    }
                }
            }
            //WE WILL ASSIGN PEOPLE TO THE GROUPS LATER.
        } else {
            throw new DataServiceException("The experiment for the group does not exist");
        }
    }

    @Override
    public boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId) {
        return allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId,groupId);
    }

    //A little weird code but it assigns the right Experiments, Groups and Conditions without repetition.
    private void assignGroups(List<Group> groups, Experiment experiment) {
        List<ExposureGroupCondition> exposureGroupConditionList = new ArrayList<>();
        for (Exposure exposure: experiment.getExposures()){
            for (Condition condition:experiment.getConditions()){
                ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
                exposureGroupCondition.setExposure(exposure);
                exposureGroupCondition.setCondition(condition);
                exposureGroupConditionList.add(exposureGroupCondition);
            }
        }
        for (int loop_number=0;loop_number<experiment.getExposures().size();loop_number++){
            for (int i=0; i<groups.size();i++){
                int groupIndex = (i+loop_number)%groups.size();
                int exposureGroupConditionIndex = loop_number*groups.size() + i;
                    exposureGroupConditionList.get(exposureGroupConditionIndex).setGroup(groups.get(groupIndex));
                    saveExposureGroupCondition(exposureGroupConditionList.get(exposureGroupConditionIndex));
            }
        }
    }

    private List<Group> createGroups(int numberOfGroups,Experiment experiment ){
        List<Group> groups = new ArrayList<>();
        for (int i=0;i<numberOfGroups;i++){
            Group group = new Group();
            group.setExperiment(experiment);
            int groupNumber = i+1;
            group.setName("Group " + groupNumber);
            Group savedGroup = save(group);
            groups.add(savedGroup);
        }
        return groups;
    }

}
