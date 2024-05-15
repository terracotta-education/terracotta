package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.GroupRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@SuppressWarnings({"PMD.PreserveStackTrace", "squid:S1192"})
public class GroupServiceImpl implements GroupService {

    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ParticipantService participantService;

    @Override
    public List<Group> findAllByExperimentId(long experimentId) {
        return groupRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<GroupDto> getGroups(Long experimentId) {
        return  CollectionUtils.emptyIfNull(findAllByExperimentId(experimentId)).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public Group getGroup(Long id) {
        return groupRepository.findByGroupId(id);
    }

    @Override
    public GroupDto postGroup(GroupDto groupDto, long experimentId) throws IdInPostException, DataServiceException{
        if (groupDto.getGroupId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        groupDto.setExperimentId(experimentId);

        try {
            return toDto(groupRepository.save(fromDto(groupDto)));
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create group:" + e.getMessage());
        }
    }

    @Override
    public GroupDto toDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupId(group.getGroupId());
        groupDto.setExperimentId(group.getExperiment().getExperimentId());
        groupDto.setName(group.getName());

        groupDto.setParticipants(
            CollectionUtils.emptyIfNull(participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(groupDto.getExperimentId(), group.getGroupId()))
                .stream()
                .filter(participant -> !participant.isTestStudent())
                .map(participant -> participantService.toDto(participant))
                .toList()
        );

        return groupDto;
    }

    @Override
    public Group fromDto(GroupDto groupDto) throws DataServiceException {
        Group group = new Group();
        group.setGroupId(groupDto.getGroupId());
        Optional<Experiment> experiment = experimentRepository.findById(groupDto.getExperimentId());

        if (experiment.isEmpty()) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        group.setExperiment(experiment.get());
        group.setName(groupDto.getName());

        return group;
    }

    @Override
    public void updateGroup(Long groupId, GroupDto groupDto) throws TitleValidationException {
        Group group = getGroup(groupId);

        if (StringUtils.isAnyBlank(groupDto.getName(), group.getName())) {
            throw new TitleValidationException("Error 100: Please give the group a name.");
        }

        if (StringUtils.isNotBlank(groupDto.getName()) && groupDto.getName().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }

        group.setName(groupDto.getName());
        groupRepository.saveAndFlush(group);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        groupRepository.deleteByGroupId(id);
    }

    // TODO ASSIGN STUDENTS TO GROUPS WILL HAPPEN IN THE FIRST LAUNCH OF THE STUDENT EXCEPT IF MANUAL

    @Override
    @Transactional
    public void createAndAssignGroupsToConditionsAndExposures(Long experimentId, SecuredInfo securedInfo, boolean isCustom) throws DataServiceException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        int numberOfGroups = experiment.get().getConditions().size();
        List<Group> groups = groupRepository.findByExperiment_ExperimentId(experimentId);

        if (groups.isEmpty()) {
            // create the groups don't assign people to them
            groups = createGroups(numberOfGroups, experiment.get());
        } else {
            if (groups.size() != experiment.get().getConditions().size()) {
                if (experiment.get().isStarted()) {
                    // should never happen, but... just in case
                    throw new DataServiceException("Error 110: The experiment has started but there is an error with the group amount");
                }

                // reset the groups for each participant
                CollectionUtils.emptyIfNull(experiment.get().getParticipants()).stream()
                    .forEach(
                        participant -> {
                            participant.setGroup(null);
                            participantRepository.save(participant);
                        }
                    );

                // delete the groups
                exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                groupRepository.deleteByExperiment_ExperimentId(experimentId);

                // create them again
                groups = createGroups(numberOfGroups, experiment.get());
            }
        }

        // assign the groups to the conditions and exposures
        List<ExposureGroupCondition> exposureGroupConditionList = exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experimentId);
        experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        if (exposureGroupConditionList.isEmpty()) {
            assignGroups(groups, experiment.get());
            return;
        }

        if (exposureGroupConditionList.size() != experiment.get().getConditions().size() * experiment.get().getExposures().size()) {
            if (experiment.get().isStarted()) {
                throw new DataServiceException("Error 110: The experiment has started but there is an error with the group/exposure/condition associations amount");
            }

            exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
            assignGroups(groups, experiment.get());
        }
        // ...populate the groups later
    }

    // assign the right Experiments, Groups, and Conditions without repetition.
    private void assignGroups(List<Group> groups, Experiment experiment) {
        List<ExposureGroupCondition> existingExposureGroupConditions = exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());
        List<ExposureGroupCondition> exposureGroupConditionList = new ArrayList<>();

        for (Exposure exposure : experiment.getExposures()) {
            for (Condition condition : experiment.getConditions()) {
                boolean exists = existingExposureGroupConditions.stream()
                    .anyMatch(
                        existingExposureGroupCondition ->
                            condition.getConditionId().equals(existingExposureGroupCondition.getCondition().getConditionId())
                            && exposure.getExposureId().equals(existingExposureGroupCondition.getExposure().getExposureId())
                        );

                if (exists) {
                    // exists for the given exposure and condition; skip this one
                    continue;
                }

                ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
                exposureGroupCondition.setExposure(exposure);
                exposureGroupCondition.setCondition(condition);
                exposureGroupConditionList.add(exposureGroupCondition);
            }
        }

        for (int loopNum = 0; loopNum < experiment.getExposures().size(); loopNum++) {
            for (int i = 0; i < groups.size(); i++) {
                int groupIndex = (i + loopNum) % groups.size();
                int exposureGroupConditionIndex = loopNum * groups.size() + i;
                exposureGroupConditionList.get(exposureGroupConditionIndex).setGroup(groups.get(groupIndex));
                exposureGroupConditionRepository.save(exposureGroupConditionList.get(exposureGroupConditionIndex));
            }
        }
    }

    private List<Group> createGroups(int numberOfGroups, Experiment experiment ) {
        List<Group> groups = new ArrayList<>(numberOfGroups);

        for (int i = 1; i <= numberOfGroups; i++) {
            Group group = new Group();
            group.setExperiment(experiment);
            group.setName(String.format("Group %s", i));
            groups.add(groupRepository.save(group));
        }

        return groups;
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException{
        if (StringUtils.isNotBlank(title) && title.length() > 255) {
            throw new TitleValidationException("Error 101: Title must be 255 characters or less.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long groupId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiment/{experimentId}/groups/{id}").buildAndExpand(experimentId, groupId).toUri());

        return headers;
    }

}
