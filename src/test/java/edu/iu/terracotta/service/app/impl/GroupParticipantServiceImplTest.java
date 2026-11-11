package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.projection.GroupParticipantCount;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.utils.TextConstants;

public class GroupParticipantServiceImplTest extends BaseTest {

    @InjectMocks private GroupParticipantServiceImpl groupParticipantService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testGetUniqueGroupByConditionId() throws GroupNotMatchingException, AssignmentNotMatchingException {
        Group group = groupParticipantService.getUniqueGroupByConditionId(1L, "1", 1l);

        assertNotNull(group);
    }

    @Test
    public void testGetUniqueGroupByConditionIdNoAssignment() {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(AssignmentNotMatchingException.class, () -> { groupParticipantService.getUniqueGroupByConditionId(1L, "1", 1l); });

        assertEquals(TextConstants.ASSIGNMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetUniqueGroupByConditionIdNoExposureGroup() {
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupNotMatchingException.class, () -> { groupParticipantService.getUniqueGroupByConditionId(1L, "1", 1l); });

        assertEquals("Error 130: This assignment does not have a condition assigned for the participant group.", exception.getMessage());
    }

    @Test
    public void testNextGroupEven() {
        Group retVal = groupParticipantService.nextGroup(experiment);

        assertNotNull(retVal);
    }

    @Test
    public void testNextGroupCustom() {
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);

        Group retVal = groupParticipantService.nextGroup(experiment);

        assertNotNull(retVal);
    }

    @Test
    public void testNextGroupUsesSingleBatchedCountQueryInsteadOfOnePerGroup() {
        Group secondGroup = mock(Group.class);
        when(secondGroup.getGroupId()).thenReturn(2L);

        GroupParticipantCount firstGroupCount = mock(GroupParticipantCount.class);
        when(firstGroupCount.getGroupId()).thenReturn(1L);
        when(firstGroupCount.getParticipantCount()).thenReturn(3L);

        GroupParticipantCount secondGroupCount = mock(GroupParticipantCount.class);
        when(secondGroupCount.getGroupId()).thenReturn(2L);
        when(secondGroupCount.getParticipantCount()).thenReturn(7L);

        when(groupRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Arrays.asList(group, secondGroup));
        when(participantRepository.countByExperiment_ExperimentIdGroupByGroup(anyLong())).thenReturn(Arrays.asList(firstGroupCount, secondGroupCount));

        Group retVal = groupParticipantService.nextGroup(experiment);

        assertNotNull(retVal);
        // one grouped query for all groups instead of one countByGroup_GroupId call per group
        verify(participantRepository, times(1)).countByExperiment_ExperimentIdGroupByGroup(anyLong());
        verify(participantRepository, never()).countByGroup_GroupId(anyLong());
    }

    // when every group is exactly on its target, the pick used to be from every group, so a 0%
    // condition got a participant (e.g. the second student of a 100 / 0 experiment, half the time)
    @Test
    public void testNextGroupNeverPicksAZeroPercentConditionWhenEveryGroupIsOnTarget() {
        GroupParticipantServiceImpl service = zeroPercentService(new float[] {100f, 0f}, new long[] {1, 0});

        for (int i = 0; i < 200; i++) {
            assertEquals(1L, service.nextGroup(zeroPercentExperiment).getGroupId());
        }
    }

    @Test
    public void testNextGroupPicksOnlyAboveZeroConditionsWhenSeveralAreOnTarget() {
        GroupParticipantServiceImpl service = zeroPercentService(new float[] {50f, 50f, 0f}, new long[] {1, 1, 0});

        for (int i = 0; i < 200; i++) {
            assertNotEquals(3L, service.nextGroup(zeroPercentExperiment).getGroupId());
        }
    }

    @Test
    public void testNextGroupStillPicksAGroupWhenEveryConditionIsZeroPercent() {
        GroupParticipantServiceImpl service = zeroPercentService(new float[] {0f, 0f}, new long[] {0, 0});

        assertNotNull(service.nextGroup(zeroPercentExperiment));
    }

    private Experiment zeroPercentExperiment;

    private GroupParticipantServiceImpl zeroPercentService(float[] pcts, long[] counts) {
        ExposureGroupConditionRepository egcRepository = mock(ExposureGroupConditionRepository.class);
        GroupRepository groupRepo = mock(GroupRepository.class);
        ParticipantRepository participantRepo = mock(ParticipantRepository.class);
        zeroPercentExperiment = mock(Experiment.class);
        Exposure exposure = mock(Exposure.class);
        when(zeroPercentExperiment.getExperimentId()).thenReturn(1L);
        when(zeroPercentExperiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(zeroPercentExperiment.getExposures()).thenReturn(List.of(exposure));
        when(exposure.getExposureId()).thenReturn(1L);

        List<Group> groups = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        List<ExposureGroupCondition> exposureGroupConditions = new ArrayList<>();
        List<GroupParticipantCount> groupCounts = new ArrayList<>();

        for (int i = 0; i < pcts.length; i++) {
            Group mockGroup = mock(Group.class);
            when(mockGroup.getGroupId()).thenReturn((long) i + 1);
            Condition mockCondition = mock(Condition.class);
            when(mockCondition.getDistributionPct()).thenReturn(pcts[i]);
            ExposureGroupCondition mockExposureGroupCondition = mock(ExposureGroupCondition.class);
            when(mockExposureGroupCondition.getGroup()).thenReturn(mockGroup);
            when(mockExposureGroupCondition.getCondition()).thenReturn(mockCondition);
            groups.add(mockGroup);
            conditions.add(mockCondition);
            exposureGroupConditions.add(mockExposureGroupCondition);

            if (counts[i] > 0) {
                GroupParticipantCount groupCount = mock(GroupParticipantCount.class);
                when(groupCount.getGroupId()).thenReturn((long) i + 1);
                when(groupCount.getParticipantCount()).thenReturn(counts[i]);
                groupCounts.add(groupCount);
            }
        }

        when(zeroPercentExperiment.getConditions()).thenReturn(conditions);
        when(groupRepo.findByExperiment_ExperimentId(anyLong())).thenReturn(groups);
        when(egcRepository.findByExposure_ExposureId(anyLong())).thenReturn(exposureGroupConditions);
        when(participantRepo.countByExperiment_ExperimentIdGroupByGroup(anyLong())).thenReturn(groupCounts);

        return new GroupParticipantServiceImpl(mock(AssignmentRepository.class), egcRepository, groupRepo, participantRepo);
    }

}
