package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.dto.GroupDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

public class GroupControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long GROUP_ID = 2L;

    private GroupController groupController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiJwtService below also collides with CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mock and left apiJwtService
        // calls silently unstubbed.
        groupController = new GroupController(groupService, apiJwtService);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
    }

    @Test
    void testAllGroupsByExperimentSuccess() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        GroupDto groupDto = GroupDto.builder().groupId(GROUP_ID).experimentId(EXPERIMENT_ID).name("group").build();
        when(groupService.getGroups(EXPERIMENT_ID, securedInfo)).thenReturn(List.of(groupDto));

        ResponseEntity<List<GroupDto>> response = groupController.allGroupsByExperiment(EXPERIMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(groupDto), response.getBody());
    }

    @Test
    void testAllGroupsByExperimentEmptyReturnsNoContent() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(groupService.getGroups(EXPERIMENT_ID, securedInfo)).thenReturn(Collections.emptyList());

        ResponseEntity<List<GroupDto>> response = groupController.allGroupsByExperiment(EXPERIMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testAllGroupsByExperimentUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<GroupDto>> response = groupController.allGroupsByExperiment(EXPERIMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testAllGroupsByExperimentPropagatesExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(ExperimentNotMatchingException.class, () -> groupController.allGroupsByExperiment(EXPERIMENT_ID, httpServletRequest));
    }

    @Test
    void testGetGroupSuccess() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        GroupDto groupDto = GroupDto.builder().groupId(GROUP_ID).experimentId(EXPERIMENT_ID).name("group").build();
        when(groupService.getGroup(GROUP_ID)).thenReturn(group);
        when(groupService.toDto(group, securedInfo)).thenReturn(groupDto);

        ResponseEntity<GroupDto> response = groupController.getGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(groupDto, response.getBody());
    }

    @Test
    void testGetGroupUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        // controller builds this branch's response with a raw ResponseEntity carrying a String body
        // even though the method is declared to return ResponseEntity<GroupDto>; getBody() is only
        // safe here because we don't force it through a GroupDto-typed reference (see final report).
        ResponseEntity<GroupDto> response = groupController.getGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testGetGroupPropagatesGroupNotMatching() throws Exception {
        doThrow(new GroupNotMatchingException("not matching")).when(apiJwtService).groupAllowed(securedInfo, EXPERIMENT_ID, GROUP_ID);

        assertThrows(GroupNotMatchingException.class, () -> groupController.getGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest));
    }

    @Test
    void testPostGroupSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        GroupDto requestDto = GroupDto.builder().name("new group").build();
        GroupDto returnedDto = GroupDto.builder().groupId(GROUP_ID).experimentId(EXPERIMENT_ID).name("new group").build();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/experiments/1/groups/2");
        when(groupService.postGroup(requestDto, EXPERIMENT_ID, securedInfo)).thenReturn(returnedDto);
        when(groupService.buildHeaders(any(UriComponentsBuilder.class), eq(EXPERIMENT_ID), eq(GROUP_ID))).thenReturn(headers);

        ResponseEntity<GroupDto> response = groupController.postGroup(EXPERIMENT_ID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
        assertEquals(headers, response.getHeaders());
    }

    @Test
    void testPostGroupUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        GroupDto requestDto = GroupDto.builder().name("new group").build();

        ResponseEntity<GroupDto> response = groupController.postGroup(EXPERIMENT_ID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testPostGroupPropagatesIdInPostException() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        GroupDto requestDto = GroupDto.builder().groupId(GROUP_ID).name("new group").build();
        when(groupService.postGroup(requestDto, EXPERIMENT_ID, securedInfo)).thenThrow(new IdInPostException("id in post"));

        assertThrows(IdInPostException.class, () -> groupController.postGroup(EXPERIMENT_ID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testPostGroupPropagatesExperimentLocked() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(EXPERIMENT_ID, true);
        GroupDto requestDto = GroupDto.builder().name("new group").build();

        assertThrows(ExperimentLockedException.class, () -> groupController.postGroup(EXPERIMENT_ID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testCreateGroupsSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = groupController.createGroups(EXPERIMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(groupService).createAndAssignGroupsToConditionsAndExposures(EXPERIMENT_ID, securedInfo, false);
    }

    @Test
    void testCreateGroupsUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = groupController.createGroups(EXPERIMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testCreateGroupsPropagatesDataServiceException() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new DataServiceException("bad data")).when(groupService).createAndAssignGroupsToConditionsAndExposures(EXPERIMENT_ID, securedInfo, false);

        assertThrows(DataServiceException.class, () -> groupController.createGroups(EXPERIMENT_ID, httpServletRequest));
    }

    @Test
    void testUpdateGroupSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        GroupDto groupDto = GroupDto.builder().groupId(GROUP_ID).name("updated").build();

        ResponseEntity<Void> response = groupController.updateGroup(EXPERIMENT_ID, GROUP_ID, groupDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(groupService).updateGroup(GROUP_ID, groupDto);
    }

    @Test
    void testUpdateGroupUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        GroupDto groupDto = GroupDto.builder().groupId(GROUP_ID).name("updated").build();

        ResponseEntity<Void> response = groupController.updateGroup(EXPERIMENT_ID, GROUP_ID, groupDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testUpdateGroupPropagatesTitleValidation() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        GroupDto groupDto = GroupDto.builder().groupId(GROUP_ID).name("bad title").build();
        doThrow(new TitleValidationException("bad title")).when(groupService).updateGroup(GROUP_ID, groupDto);

        assertThrows(TitleValidationException.class, () -> groupController.updateGroup(EXPERIMENT_ID, GROUP_ID, groupDto, httpServletRequest));
    }

    @Test
    void testDeleteGroupSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = groupController.deleteGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(groupService).deleteById(GROUP_ID);
    }

    @Test
    void testDeleteGroupEmptyResultDataAccessExceptionReturnsNotFound() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(groupService).deleteById(GROUP_ID);

        ResponseEntity<Void> response = groupController.deleteGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteGroupUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = groupController.deleteGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testDeleteGroupPropagatesExperimentLocked() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(EXPERIMENT_ID, true);

        assertThrows(ExperimentLockedException.class, () -> groupController.deleteGroup(EXPERIMENT_ID, GROUP_ID, httpServletRequest));
    }

}
