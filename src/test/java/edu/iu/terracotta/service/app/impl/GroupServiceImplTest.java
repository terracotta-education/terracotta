package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.dto.GroupDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

public class GroupServiceImplTest extends BaseTest {

    @InjectMocks private GroupServiceImpl groupService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(group.getExperiment()).thenReturn(experiment);
    }

    @Test
    public void testFindAllByExperimentId() {
        List<Group> retVal = groupService.findAllByExperimentId(1L);

        assertEquals(1, retVal.size());
        assertEquals(group, retVal.get(0));
    }

    @Test
    public void testGetGroups() {
        when(participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(anyLong(), anyLong())).thenReturn(List.of(participant));
        when(participantService.toDto(eq(participant), anyList(), eq(securedInfo))).thenReturn(participantDto);

        List<GroupDto> retVal = groupService.getGroups(1L, securedInfo);

        assertEquals(1, retVal.size());
        assertEquals(1, retVal.get(0).getParticipants().size());
    }

    @Test
    public void testGetGroup() {
        when(groupRepository.findByGroupId(anyLong())).thenReturn(group);

        Group retVal = groupService.getGroup(1L);

        assertEquals(group, retVal);
    }

    @Test
    public void testPostGroupSuccess() throws IdInPostException, DataServiceException {
        GroupDto groupDto = GroupDto.builder().name("Group A").build();

        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(anyLong(), anyLong())).thenReturn(Collections.emptyList());

        GroupDto retVal = groupService.postGroup(groupDto, 1L, securedInfo);

        assertNotNull(retVal);
        assertEquals(1L, retVal.getGroupId());
    }

    @Test
    public void testPostGroupIdInPostExceptionThrows() {
        GroupDto groupDto = GroupDto.builder().groupId(5L).build();

        Exception exception = assertThrows(IdInPostException.class, () -> groupService.postGroup(groupDto, 1L, securedInfo));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostGroupExperimentNotFoundThrows() {
        GroupDto groupDto = GroupDto.builder().build();
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> groupService.postGroup(groupDto, 1L, securedInfo));

        assertEquals("Error 105: Unable to create group:The experiment for the group does not exist", exception.getMessage());
    }

    @Test
    public void testToDtoFiltersTestStudents() {
        Participant testStudentParticipant = mock(Participant.class);
        when(testStudentParticipant.isTestStudent()).thenReturn(true);

        when(participantRepository.findByExperiment_ExperimentIdAndGroup_GroupId(anyLong(), anyLong())).thenReturn(Arrays.asList(participant, testStudentParticipant));
        when(participantService.toDto(eq(participant), anyList(), eq(securedInfo))).thenReturn(participantDto);

        GroupDto retVal = groupService.toDto(group, securedInfo);

        assertEquals(1, retVal.getParticipants().size());
        verify(participantService, never()).toDto(eq(testStudentParticipant), anyList(), any());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        GroupDto groupDto = GroupDto.builder().groupId(1L).experimentId(1L).name("Group A").build();

        Group retVal = groupService.fromDto(groupDto);

        assertEquals(1L, retVal.getGroupId());
        assertEquals("Group A", retVal.getName());
        assertEquals(experiment, retVal.getExperiment());
    }

    @Test
    public void testFromDtoExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());
        GroupDto groupDto = GroupDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> groupService.fromDto(groupDto));

        assertEquals("The experiment for the group does not exist", exception.getMessage());
    }

    @Test
    public void testUpdateGroupSuccess() throws TitleValidationException {
        when(groupRepository.findByGroupId(anyLong())).thenReturn(group);
        when(group.getName()).thenReturn("Old Name");
        GroupDto groupDto = GroupDto.builder().name("New Name").build();

        groupService.updateGroup(1L, groupDto);

        verify(group).setName("New Name");
        verify(groupRepository).saveAndFlush(group);
    }

    @Test
    public void testUpdateGroupBlankNameThrows() {
        when(groupRepository.findByGroupId(anyLong())).thenReturn(group);
        when(group.getName()).thenReturn("Old Name");
        GroupDto groupDto = GroupDto.builder().name("").build();

        Exception exception = assertThrows(TitleValidationException.class, () -> groupService.updateGroup(1L, groupDto));

        assertEquals("Error 100: Please give the group a name.", exception.getMessage());
    }

    @Test
    public void testUpdateGroupNameTooLongThrows() {
        when(groupRepository.findByGroupId(anyLong())).thenReturn(group);
        when(group.getName()).thenReturn("Old Name");
        GroupDto groupDto = GroupDto.builder().name("a".repeat(256)).build();

        Exception exception = assertThrows(TitleValidationException.class, () -> groupService.updateGroup(1L, groupDto));

        assertEquals("Error 101: The title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testDeleteById() {
        groupService.deleteById(1L);

        verify(groupRepository).deleteByGroupId(1L);
    }

    @Test
    public void testValidateTitleValid() {
        assertDoesNotThrow(() -> groupService.validateTitle("Valid Title"));
    }

    @Test
    public void testValidateTitleTooLongThrows() {
        Exception exception = assertThrows(TitleValidationException.class, () -> groupService.validateTitle("a".repeat(256)));

        assertEquals("Error 101: Title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders retVal = groupService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L);

        assertNotNull(retVal);
    }

    @Test
    public void testCreateAndAssignGroupsToConditionsAndExposuresExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false));

        assertEquals("The experiment for the group does not exist", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAndAssignGroupsToConditionsAndExposuresCreatesGroupsWithSaveAll() throws DataServiceException {
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        when(groupRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Arrays.asList(exposureGroupCondition, exposureGroupCondition));

        groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false);

        // groups are batched into a single saveAll instead of one save() per group
        verify(groupRepository, never()).save(any(Group.class));

        ArgumentCaptor<List<Group>> groupsCaptor = ArgumentCaptor.forClass(List.class);
        verify(groupRepository).saveAll(groupsCaptor.capture());
        assertEquals(2, groupsCaptor.getValue().size());
    }

    @Test
    public void testCreateAndAssignGroupsToConditionsAndExposuresMismatchedCountWhileStartedThrows() {
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        when(experiment.isStarted()).thenReturn(true);
        when(groupRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(group));

        Exception exception = assertThrows(DataServiceException.class, () -> groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false));

        assertEquals("Error 110: The experiment has started but there is an error with the group amount", exception.getMessage());
        verify(groupRepository, never()).deleteByExperiment_ExperimentId(anyLong());
    }

    @Test
    public void testCreateAndAssignGroupsToConditionsAndExposuresResetsGroupsWhenMismatchedAndNotStarted() throws DataServiceException {
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        when(experiment.isStarted()).thenReturn(false);
        when(experiment.getParticipants()).thenReturn(Collections.singletonList(participant));
        when(groupRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(group));
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Arrays.asList(exposureGroupCondition, exposureGroupCondition));

        groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false);

        verify(participant).setGroup(null);
        verify(participantRepository).saveAll(anyList());
        verify(exposureGroupConditionRepository).deleteByExposure_Experiment_ExperimentId(1L);
        verify(groupRepository).deleteByExperiment_ExperimentId(1L);
        verify(groupRepository).saveAll(anyList());
    }

    @Test
    public void testCreateAndAssignGroupsToConditionsAndExposuresAssignsGroupsWhenNoneExist() throws DataServiceException {
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());

        groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false);

        verify(exposureGroupConditionRepository).saveAll(anyList());
    }

    @Test
    public void testCreateAndAssignGroupsToConditionsAndExposuresMismatchWhileStartedThrows() {
        when(experiment.isStarted()).thenReturn(true);
        when(experiment.getExposures()).thenReturn(Arrays.asList(exposure, exposure));
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));

        Exception exception = assertThrows(DataServiceException.class, () -> groupService.createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false));

        assertEquals("Error 110: The experiment has started but there is an error with the group/exposure/condition associations amount", exception.getMessage());
    }

}
