package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import edu.iu.terracotta.config.ContextProvider;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;

public class UserExtendedImplTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private LmsUserBatchWriteService lmsUserBatchWriteService;

    private UserExtendedImpl userExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        ContextProvider contextProvider = new ContextProvider();
        contextProvider.setApplicationContext(applicationContext);

        when(applicationContext.getBean(LmsUserBatchWriteService.class)).thenReturn(lmsUserBatchWriteService);

        userExtended = new UserExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    @Test
    public void testGetUsersInCourseStartsBatchBeforeFetching() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        UUID batchId = UUID.randomUUID();
        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), batchId);

        verify(lmsUserBatchWriteService).startBatch(batchId);
    }

    @Test
    public void testGetUsersInCourseSavesAllUsersInOneBatchPerPage() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[{\"id\":1,\"email\":\"a@example.com\"},{\"id\":2,\"email\":\"b@example.com\"},{\"id\":3,\"email\":\"c@example.com\"}]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), UUID.randomUUID());

        // all users from the page are saved in a single saveUsers() call, never one at a time
        verify(lmsUserBatchWriteService, times(1)).saveUsers(any());
    }

    @Test
    public void testGetUsersInCourseSavesOncePerPage() throws Exception {
        Response pageOne = new Response();
        pageOne.setErrorHappened(false);
        pageOne.setResponseCode(200);
        pageOne.setContent("[{\"id\":1,\"email\":\"a@example.com\"}]");
        pageOne.setNextLink("https://canvas.example.com/api/v1/courses/1/users?page=2");

        Response pageTwo = new Response();
        pageTwo.setErrorHappened(false);
        pageTwo.setResponseCode(200);
        pageTwo.setContent("[{\"id\":2,\"email\":\"b@example.com\"}]");
        pageTwo.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenReturn(pageOne)
            .thenReturn(pageTwo);

        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), UUID.randomUUID());

        // one saveUsers() per page fetched, committed independently of the caller's transaction
        verify(lmsUserBatchWriteService, times(2)).saveUsers(any());
    }

    @Test
    public void testGetUsersInCourseErrorMarksBatchProcessingFailed() throws Exception {
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(500);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        UUID batchId = UUID.randomUUID();
        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), batchId);

        verify(lmsUserBatchWriteService).markFailed(eq(batchId), any());
        verify(lmsUserBatchWriteService, never()).saveUsers(any());
    }

    // A thrown exception (e.g. an OAuth failure that outlives the token's lifetime mid-fetch)
    // must also durably mark the batch failed - not just a non-exceptional bad HTTP response -
    // and must still propagate so the caller's own error handling is unaffected.
    @Test
    public void testGetUsersInCourseThrownExceptionMarksBatchProcessingFailedAndPropagates() throws Exception {
        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException());

        UUID batchId = UUID.randomUUID();

        assertThrows(
            InvalidOauthTokenException.class,
            () -> userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), batchId)
        );

        verify(lmsUserBatchWriteService).markFailed(eq(batchId), any());
    }

    @Test
    public void testGetUsersInCourseNoUsersStillCallsSaveUsers() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), UUID.randomUUID());

        verify(lmsUserBatchWriteService, times(1)).saveUsers(List.<LmsUserBatch>of());
    }

}
