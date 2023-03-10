package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
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
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@SuppressWarnings({"PMD.PreserveStackTrace", "squid:S1192"})
public class GroupServiceImpl implements GroupService {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private ParticipantService participantService;

    private Random random = new Random();

    @Override
    public List<Group> findAllByExperimentId(long experimentId) {
        return allRepositories.groupRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<GroupDto> getGroups(Long experimentId) {
        return  CollectionUtils.emptyIfNull(findAllByExperimentId(experimentId)).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public Group getGroup(Long id) {
        return allRepositories.groupRepository.findByGroupId(id);
    }

    @Override
    public GroupDto postGroup(GroupDto groupDto, long experimentId) throws IdInPostException, DataServiceException{
        if (groupDto.getGroupId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        groupDto.setExperimentId(experimentId);

        try{
            return toDto(save(fromDto(groupDto)));
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
            CollectionUtils.emptyIfNull(allRepositories.participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(groupDto.getExperimentId(), group.getGroupId()))
                .stream()
                .map(participant -> participantService.toDto(participant))
                .toList()
        );

        return groupDto;
    }

    @Override
    public Group fromDto(GroupDto groupDto) throws DataServiceException {
        Group group = new Group();
        group.setGroupId(groupDto.getGroupId());
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(groupDto.getExperimentId());

        if (!experiment.isPresent()) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        group.setExperiment(experiment.get());
        group.setName(groupDto.getName());

        return group;
    }

    @Override
    public Group save(Group group) {
        return allRepositories.groupRepository.save(group);
    }

    @Override
    public ExposureGroupCondition saveExposureGroupCondition(ExposureGroupCondition exposureGroupCondition) {
        return allRepositories.exposureGroupConditionRepository.save(exposureGroupCondition);
    }

    @Override
    public Optional<Group> findById(Long id) {
        return allRepositories.groupRepository.findById(id);
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
        saveAndFlush(group);
    }

    @Override
    public void saveAndFlush(Group groupToChange) {
        allRepositories.groupRepository.saveAndFlush(groupToChange);
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.groupRepository.deleteByGroupId(id);
    }

    @Override
    public boolean groupBelongsToExperiment(Long experimentId, Long groupId) {
        return allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId, groupId);
    }

    // TODO ASSIGN STUDENTS TO GROUPS WILL HAPPEN IN THE FIRST LAUNCH OF THE STUDENT EXCEPT IF MANUAL

    @Override
    @Transactional
    public void createAndAssignGroupsToConditionsAndExposures(Long experimentId, SecuredInfo securedInfo, boolean isCustom) throws DataServiceException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
            throw new DataServiceException("The experiment for the group does not exist");
        }

        int numberOfGroups = experiment.get().getConditions().size();
        List<Group> groups = allRepositories.groupRepository.findByExperiment_ExperimentId(experimentId);

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
                            participantService.save(participant);
                        }
                    );

                // delete the groups
                allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                allRepositories.groupRepository.deleteByExperiment_ExperimentId(experimentId);

                // create them again
                groups = createGroups(numberOfGroups, experiment.get());
            }
        }

        // assign the groups to the conditions and exposures
        List<ExposureGroupCondition> exposureGroupConditionList = allRepositories.exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experimentId);
        experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
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

            allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
            assignGroups(groups, experiment.get());
        }
        // ...populate the groups later
    }

    @Override
    public boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId) {
        return allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId,groupId);
    }

    // assign the right Experiments, Groups, and Conditions without repetition.
    private void assignGroups(List<Group> groups, Experiment experiment) {
        List<ExposureGroupCondition> existingExposureGroupConditions = allRepositories.exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());
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
                saveExposureGroupCondition(exposureGroupConditionList.get(exposureGroupConditionIndex));
            }
        }
    }

    private List<Group> createGroups(int numberOfGroups, Experiment experiment ) {
        List<Group> groups = new ArrayList<>(numberOfGroups);

        for (int i = 1; i <= numberOfGroups; i++) {
            Group group = new Group();
            group.setExperiment(experiment);
            group.setName(String.format("Group %s", i));
            groups.add(save(group));
        }

        return groups;
    }

    @Override
    public Group nextGroup(Experiment experiment) {
        AtomicLong totalParticipants = new AtomicLong(0);

        Map<Long, Long> count = CollectionUtils.emptyIfNull(allRepositories.groupRepository.findByExperiment_ExperimentId(experiment.getExperimentId()))
            .stream()
            .collect(
                Collectors.toMap(
                    Group::getGroupId,
                    group -> {
                        long groupCount = allRepositories.participantRepository.countByGroup_GroupId(group.getGroupId());
                        totalParticipants.addAndGet(groupCount);

                        return groupCount;
                    }
                )
            );

        /**
         *  If the experiment has just one exposure, we look at the groups/Exposures/etc to see the group assigned to the condition.
         *  If the experiment has more than one exposure, we shouldn't be doing this.
         */
        List<ExposureGroupCondition> exposureGroupConditionList =
            allRepositories.exposureGroupConditionRepository.findByExposure_ExposureId(experiment.getExposures().get(0).getExposureId());

        List<Group> unbalancedGroups = CollectionUtils.emptyIfNull(exposureGroupConditionList).stream()
            .filter(
                exposureGroupCondition -> {
                    Long countGroup = count.get(exposureGroupCondition.getGroup().getGroupId());
                    float groupUnbalancement;

                    if (DistributionTypes.EVEN.equals(experiment.getDistributionType())) {
                        float evenPercent = 100f / experiment.getConditions().size();

                        if (totalParticipants.get() != 0) {
                            groupUnbalancement = evenPercent - (100 * (countGroup / (float) totalParticipants.get()));
                        } else {
                            groupUnbalancement = evenPercent;
                        }
                    } else {
                        if (totalParticipants.get() != 0) {
                            groupUnbalancement = exposureGroupCondition.getCondition().getDistributionPct() - (100 * (countGroup / (float) totalParticipants.get()));
                        } else {
                            groupUnbalancement = exposureGroupCondition.getCondition().getDistributionPct();
                        }
                    }

                    return groupUnbalancement > 0;
                }
            )
            .map(ExposureGroupCondition::getGroup)
            .toList();

        if (CollectionUtils.isEmpty(unbalancedGroups)) {
            /**
             *  No unbalanced groups exist. Pick a random group from all available groups;
             *  index is chosen via Java's random number generator
             */
            return exposureGroupConditionList.get(random.nextInt(exposureGroupConditionList.size())).getGroup();
        }

        /**
         *  Pick a random group from the available unbalanced groups;
         *  index is chosen via Java's random number generator
         */
        return unbalancedGroups.get(random.nextInt(unbalancedGroups.size()));
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

    @Override
    public Group getUniqueGroupByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws GroupNotMatchingException, AssignmentNotMatchingException {
        Assignment assignment = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, canvasAssignmentId);

        if (assignment == null) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }

        Optional<ExposureGroupCondition> exposureGroupCondition = allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(conditionId, assignment.getExposure().getExposureId());

        if (!exposureGroupCondition.isPresent()) {
            throw new GroupNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }

        return exposureGroupCondition.get().getGroup();
    }

}
