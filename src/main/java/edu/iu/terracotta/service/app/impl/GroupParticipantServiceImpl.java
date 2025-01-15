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

import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.service.app.GroupParticipantService;
import edu.iu.terracotta.utils.TextConstants;

@Service
public class GroupParticipantServiceImpl implements GroupParticipantService {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private ParticipantRepository participantRepository;

    private Random random = new Random();

    @Override
    public Group getUniqueGroupByConditionId(Long experimentId, String lmsAssignmentId, Long conditionId) throws GroupNotMatchingException, AssignmentNotMatchingException {
        Assignment assignment = assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, lmsAssignmentId);

        if (assignment == null) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }

        Optional<ExposureGroupCondition> exposureGroupCondition = exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(conditionId, assignment.getExposure().getExposureId());

        if (exposureGroupCondition.isEmpty()) {
            throw new GroupNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }

        return exposureGroupCondition.get().getGroup();
    }

    @Override
    public Group nextGroup(Experiment experiment) {
        AtomicLong totalParticipants = new AtomicLong(0);

        Map<Long, Long> count = CollectionUtils.emptyIfNull(groupRepository.findByExperiment_ExperimentId(experiment.getExperimentId()))
            .stream()
            .collect(
                Collectors.toMap(
                    Group::getGroupId,
                    group -> {
                        long groupCount = participantRepository.countByGroup_GroupId(group.getGroupId());
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
            exposureGroupConditionRepository.findByExposure_ExposureId(experiment.getExposures().get(0).getExposureId());

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
