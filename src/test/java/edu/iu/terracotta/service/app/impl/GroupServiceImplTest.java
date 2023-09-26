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

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.utils.TextConstants;

public class GroupServiceImplTest extends BaseTest {

    @InjectMocks private GroupServiceImpl groupService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
    }

    @Test
    public void testgetUniqueGroupByConditionId() throws GroupNotMatchingException, AssignmentNotMatchingException {
        Group group = groupService.getUniqueGroupByConditionId(1L, "1", 1l);

        assertNotNull(group);
    }

    @Test
    public void testgetUniqueGroupByConditionIdNoAssignment() {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);

        Exception exception = assertThrows(AssignmentNotMatchingException.class, () -> { groupService.getUniqueGroupByConditionId(1L, "1", 1l); });

        assertEquals(TextConstants.ASSIGNMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testgetUniqueGroupByConditionIdNoExposureGroup() {
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(GroupNotMatchingException.class, () -> { groupService.getUniqueGroupByConditionId(1L, "1", 1l); });

        assertEquals("Error 130: This assignment does not have a condition assigned for the participant group.", exception.getMessage());
    }

}
