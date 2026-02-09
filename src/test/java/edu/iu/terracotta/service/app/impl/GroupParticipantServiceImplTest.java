package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
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

}
