package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.dto.GroupDto;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.PreserveStackTrace", "squid:S1192"})
public class GroupServiceImpl implements GroupService {

    private final ExperimentRepository experimentRepository;
    private final ExposureGroupConditionRepository exposureGroupConditionRepository;
    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantService participantService;

    @Override
    public List<Group> findAllByExperimentId(long experimentId) {
        return groupRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<GroupDto> getGroups(Long experimentId, SecuredInfo securedInfo) {
        return  CollectionUtils.emptyIfNull(findAllByExperimentId(experimentId)).stream()
            .map(group -> toDto(group, securedInfo))
            .toList();
    }

    @Override
    public Group getGroup(Long id) {
        return groupRepository.findByGroupId(id);
    }

    @Override
    public Group getGroupByUuid(UUID uuid) throws GroupNotMatchingException {
        return Optional.ofNullable(groupRepository.findByUuid(uuid))
            .orElseThrow(() -> new GroupNotMatchingException(TextConstants.GROUP_NOT_MATCHING));
    }

    @Override
    public long getGroupIdByUuid(UUID uuid) throws GroupNotMatchingException {
        return groupRepository.findIdByUuid(uuid)
            .orElseThrow(() -> new GroupNotMatchingException(TextConstants.GROUP_NOT_MATCHING));
    }

    @Override
    public GroupDto postGroup(GroupDto groupDto, long experimentId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException{
        if (groupDto.getGroupId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        Experiment experiment = experimentRepository.findById(experimentId).orElse(null);
        groupDto.setExperimentId(experiment != null ? experiment.getUuid() : null);

        try {
            return toDto(groupRepository.save(fromDto(groupDto)), securedInfo);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create group:" + e.getMessage());
        }
    }

    @Override
    public GroupDto toDto(Group group, SecuredInfo securedInfo) {
        GroupDto groupDto = new GroupDto();
        groupDto.setGroupId(group.getUuid());
        groupDto.setExperimentId(group.getExperiment().getUuid());
        groupDto.setName(group.getName());

        List<Long> publishedExperimentAssignmentIds = participantService.calculatedPublishedAssignmentIds(group.getExperiment().getExperimentId(), securedInfo.getLmsCourseId(), group.getExperiment().getCreatedBy());

        groupDto.setParticipants(
            CollectionUtils.emptyIfNull(participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(group.getExperiment().getExperimentId(), group.getGroupId()))
                .stream()
                .filter(participant -> !participant.isTestStudent())
                .map(participant -> participantService.toDto(participant, publishedExperimentAssignmentIds, securedInfo))
                .toList()
        );

        return groupDto;
    }

    @Override
    public Group fromDto(GroupDto groupDto) throws DataServiceException {
        // groupDto.getGroupId() (now a uuid) is intentionally not set on a new Group here - the
        // caller (postGroup) already rejects a create request that carries one (IdInPostException),
        // and the real numeric id/uuid are both IDENTITY/@PrePersist generated at insert time regardless.
        Group group = new Group();
        Experiment experiment = groupDto.getExperimentId() != null ? experimentRepository.findByUuid(groupDto.getExperimentId()) : null;

        if (experiment == null) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        group.setExperiment(experiment);
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
                List<edu.iu.terracotta.dao.entity.Participant> participants = new ArrayList<>(CollectionUtils.emptyIfNull(experiment.get().getParticipants()));
                participants.forEach(participant -> participant.setGroup(null));
                participantRepository.saveAll(participants);

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
            }
        }

        exposureGroupConditionRepository.saveAll(exposureGroupConditionList);
    }

    private List<Group> createGroups(int numberOfGroups, Experiment experiment ) {
        List<Group> groups = new ArrayList<>(numberOfGroups);

        for (int i = 1; i <= numberOfGroups; i++) {
            Group group = new Group();
            group.setExperiment(experiment);
            group.setName(String.format("Group %s", i));
            groups.add(group);
        }

        return groupRepository.saveAll(groups);
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException{
        if (StringUtils.isNotBlank(title) && title.length() > 255) {
            throw new TitleValidationException("Error 101: Title must be 255 characters or less.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, UUID experimentId, UUID groupId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiment/{experimentId}/groups/{id}").buildAndExpand(experimentId, groupId).toUri());

        return headers;
    }

}
