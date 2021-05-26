package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GroupServiceImpl implements GroupService {

    @Autowired
    AllRepositories allRepositories;

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
        //TODO check null?
        groupDto.setExperimentId(group.getExperiment().getExperimentId());
        groupDto.setName(group.getName());

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
    public Optional<Group> findById(Long id) { return allRepositories.groupRepository.findById(id); }

    @Override
    public void saveAndFlush(Group groupToChange) { allRepositories.groupRepository.saveAndFlush(groupToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.groupRepository.deleteById(id); }

    @Override
    public boolean groupBelongsToExperiment(Long experimentId, Long groupId) {
        return allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId,groupId);
    }

}
