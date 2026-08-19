package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.SubmissionExtended;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;

public class SubmissionExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private SubmissionExtendedImpl submissionExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        submissionExtended = new SubmissionExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    @Test
    public void testGetCourseSubmissionsReturnsParsedSubmissions() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[{\"id\":10,\"assignment_id\":100,\"user_id\":200,\"score\":95.5,\"workflow_state\":\"graded\"}]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<SubmissionExtended> results = submissionExtended.getCourseSubmissions(new GetSubmissionsOptions("1", 100L));

        assertEquals(1, results.size());
        assertEquals("100", results.get(0).getAssignmentId());
        assertEquals("200", results.get(0).getUserId());
        assertEquals(95.5, results.get(0).getScore(), 0.0001);
        assertEquals("graded", results.get(0).getState());
    }

    @Test
    public void testGetCourseSubmissionsThrowsExceptionWhenCanvasIdBlank() {
        GetSubmissionsOptions options = new GetSubmissionsOptions("", 100L);

        assertThrows(IllegalArgumentException.class, () -> submissionExtended.getCourseSubmissions(options));
    }

    @Test
    public void testGetCourseSubmissionsThrowsExceptionWhenAssignmentIdNull() {
        GetSubmissionsOptions options = new GetSubmissionsOptions("1");

        assertThrows(IllegalArgumentException.class, () -> submissionExtended.getCourseSubmissions(options));
    }

    @Test
    public void testGetCourseSubmissionsReturnsEmptyListWhenNoSubmissions() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<SubmissionExtended> results = submissionExtended.getCourseSubmissions(new GetSubmissionsOptions("1", 100L));

        assertTrue(results.isEmpty());
    }

}
