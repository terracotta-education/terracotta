package edu.iu.terracotta.service.app.async.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;

class LmsAssignmentSyncServiceImplTest {

    @Mock private ApiJwtService apiJwtService;
    @Mock private AssignmentService assignmentService;
    @Mock private AssignmentAsyncService assignmentAsyncService;
    @Mock private ExperimentCopyCandidateService experimentCopyCandidateService;
    @Mock private ExperimentCopyRecreationAsyncService experimentCopyRecreationAsyncService;

    private LmsAssignmentSyncServiceImpl lmsAssignmentSyncService;
    private SecuredInfo securedInfo;
    private List<LmsAssignment> lmsAssignments;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        lmsAssignmentSyncService = new LmsAssignmentSyncServiceImpl(apiJwtService, assignmentService, assignmentAsyncService, experimentCopyCandidateService, experimentCopyRecreationAsyncService);
        securedInfo = SecuredInfo.builder().contextId(1L).userId("instructor-1").build();
        lmsAssignments = List.of(LmsAssignment.builder().id("900").build());
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenReturn(lmsAssignments);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
    }

    // a course copy no notice reported: recreate the copied experiments as the launching
    // instructor, and hold the obsolete-assignment check back, since it would mark the copies obsolete
    @Test
    void testSyncRecreatesACopyItFindsInsteadOfMarkingItObsolete() throws Exception {
        when(experimentCopyCandidateService.stageFromCopiedAssignments(securedInfo, lmsAssignments)).thenReturn(true);

        lmsAssignmentSyncService.sync(securedInfo);

        verify(experimentCopyRecreationAsyncService).recreate(1L, "instructor-1");
        verify(assignmentAsyncService, never()).handleAssignmentTasksInLmsByContext(any(), anyList());
    }

    @Test
    void testSyncRunsTheUsualChecksWhenThereIsNoCopy() throws Exception {
        lmsAssignmentSyncService.sync(securedInfo);

        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo, lmsAssignments);
        verify(experimentCopyRecreationAsyncService, never()).recreate(anyLong(), anyString());
    }

    // recreation runs on an instructor's LMS access, so a student's launch doesn't look for copies
    @Test
    void testSyncDoesNotLookForCopiesForAStudent() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        lmsAssignmentSyncService.syncInBackground(securedInfo);

        verifyNoInteractions(experimentCopyCandidateService, experimentCopyRecreationAsyncService);
        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo, lmsAssignments);
    }

    @Test
    void testSyncSkipsWithoutLmsAuthorization() throws Exception {
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo))
            .thenThrow(new ApiException("Failed to get the list of assignments", new LmsOAuthException("no token")));

        lmsAssignmentSyncService.sync(securedInfo);

        verifyNoInteractions(experimentCopyCandidateService, assignmentAsyncService);
    }

    // the launch's page load must not fail because the LMS sync did
    @Test
    void testSyncLogsOtherFailures() throws Exception {
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenThrow(new ApiException("lms error"));
        lmsAssignmentSyncService.sync(securedInfo);

        doReturn(lmsAssignments).when(assignmentService).getAllAssignmentsForLmsCourse(securedInfo);
        doThrow(new IllegalStateException("db error")).when(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo, lmsAssignments);
        lmsAssignmentSyncService.sync(securedInfo);

        verify(experimentCopyRecreationAsyncService, never()).recreate(anyLong(), anyString());
    }

}
