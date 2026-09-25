package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;

public class CourseExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private CourseExtendedImpl courseExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        courseExtended = new CourseExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    private Response buildResponse(boolean errorHappened, int responseCode, String content, String nextLink) {
        Response response = new Response();
        response.setErrorHappened(errorHappened);
        response.setResponseCode(responseCode);
        response.setContent(content);
        response.setNextLink(nextLink);

        return response;
    }

    @Test
    public void testListCoursesForUserReturnsParsedCourses() throws Exception {
        Response response = buildResponse(
            false,
            200,
            "[{\"id\":1,\"name\":\"Course One\"},{\"id\":2,\"name\":\"Course Two\"}]",
            null
        );

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<CourseExtended> courses = courseExtended.listCoursesForUser(new ListUserCoursesOptions("9"));

        assertEquals(2, courses.size());
        assertEquals("1", courses.get(0).getId());
        assertEquals("Course One", courses.get(0).getCourse().getName());
        assertEquals("2", courses.get(1).getId());
        assertEquals(Course.class, courses.get(0).getType());
        assertEquals(Course.class, courses.get(1).getType());
    }

    @Test
    public void testListCoursesForUserBuildsUrlWithUserId() throws Exception {
        Response response = buildResponse(false, 200, "[]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        courseExtended.listCoursesForUser(new ListUserCoursesOptions("77"));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClient).sendApiGet(eq(oauthToken), urlCaptor.capture(), anyInt(), anyInt());
        assertTrue(urlCaptor.getValue().contains("users/77/courses"));
    }

    @Test
    public void testListCoursesForUserAcrossMultiplePagesAggregatesResults() throws Exception {
        Response pageOne = buildResponse(false, 200, "[{\"id\":1,\"name\":\"Page One\"}]", "https://canvas.example.com/api/v1/users/9/courses?page=2");
        Response pageTwo = buildResponse(false, 200, "[{\"id\":2,\"name\":\"Page Two\"}]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenReturn(pageOne)
            .thenReturn(pageTwo);

        List<CourseExtended> courses = courseExtended.listCoursesForUser(new ListUserCoursesOptions("9"));

        assertEquals(2, courses.size());
        assertEquals("Page One", courses.get(0).getCourse().getName());
        assertEquals("Page Two", courses.get(1).getCourse().getName());
    }

    @Test
    public void testListCoursesForUserReturnsEmptyListOnCanvasError() throws Exception {
        // Unlike the single-object fetches in the sibling Assignment/Conversation Extended classes,
        // list fetches route through BaseImpl.getListFromCanvas -> RestCanvasMessenger.getFromCanvas,
        // which checks getErrorHappened()/getResponseCode() and returns Collections.emptyList() instead
        // of throwing, so an error response here is handled gracefully with no exception.
        Response response = buildResponse(true, 500, null, null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<CourseExtended> courses = courseExtended.listCoursesForUser(new ListUserCoursesOptions("9"));

        assertTrue(courses.isEmpty());
    }

    @Test
    public void testListCoursesForUserReturnsEmptyListWhenNoCourses() throws Exception {
        Response response = buildResponse(false, 200, "[]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<CourseExtended> courses = courseExtended.listCoursesForUser(new ListUserCoursesOptions("9"));

        assertTrue(courses.isEmpty());
    }

}
