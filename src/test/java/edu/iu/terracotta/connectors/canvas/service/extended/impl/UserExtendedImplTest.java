package edu.iu.terracotta.connectors.canvas.service.extended.impl;

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
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;

import jakarta.persistence.EntityManager;

public class UserExtendedImplTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private EntityManager entityManager;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;

    private UserExtendedImpl userExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        ContextProvider contextProvider = new ContextProvider();
        contextProvider.setApplicationContext(applicationContext);

        when(applicationContext.getBean(EntityManager.class)).thenReturn(entityManager);
        when(applicationContext.getBean(LmsUserBatchRepository.class)).thenReturn(lmsUserBatchRepository);
        when(applicationContext.getBean(LmsUserBatchProcessingRepository.class)).thenReturn(lmsUserBatchProcessingRepository);
        when(lmsUserBatchProcessingRepository.saveAndFlush(any(LmsUserBatchProcessing.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        userExtended = new UserExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
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

        // all users from the page are saved in a single saveAll() call, never one at a time
        verify(lmsUserBatchRepository, times(1)).saveAll(any());
        verify(lmsUserBatchRepository, never()).save(any(LmsUserBatch.class));
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

        // one saveAll() per page fetched, still never a per-user save()
        verify(lmsUserBatchRepository, times(2)).saveAll(any());
        verify(lmsUserBatchRepository, never()).save(any(LmsUserBatch.class));
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    @Test
    public void testGetUsersInCourseErrorMarksBatchProcessingFailed() throws Exception {
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(500);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing processing = LmsUserBatchProcessing.builder().batchId(batchId).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(java.util.Optional.of(processing));

        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), batchId);

        verify(lmsUserBatchRepository, never()).saveAll(any());
        verify(lmsUserBatchRepository, never()).save(any(LmsUserBatch.class));
    }

    @Test
    public void testGetUsersInCourseNoUsersDoesNotCallSaveAll() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        userExtended.getUsersInCourse(new GetUsersInCourseOptions("1"), UUID.randomUUID());

        verify(lmsUserBatchRepository, times(1)).saveAll(List.of());
    }

}
