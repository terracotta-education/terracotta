package edu.iu.terracotta.service.app.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.GroupParticipantService;
import edu.iu.terracotta.utils.TextConstants;

@Service
public class GroupParticipantServiceImpl implements GroupParticipantService {

    @Autowired private AllRepositories allRepositories;

    private Random random = new Random();

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

}
