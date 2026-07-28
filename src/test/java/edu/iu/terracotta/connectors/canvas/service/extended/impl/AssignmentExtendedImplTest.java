package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.ksu.canvas.model.assignment.Assignment;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserAssignmentOptions;

public class AssignmentExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private AssignmentExtendedImpl assignmentExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        assignmentExtended = new AssignmentExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
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
    public void testListCourseAssignmentsReturnsParsedAndMergedAssignments() throws Exception {
        Response response = buildResponse(
            false,
            200,
            "[{\"id\":1,\"name\":\"Assignment One\"},{\"id\":2,\"name\":\"Assignment Two\"}]",
            null
        );

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<AssignmentExtended> assignments = assignmentExtended.listCourseAssignments(new ListCourseAssignmentsOptions("1"));

        assertEquals(2, assignments.size());
        assertEquals("1", assignments.get(0).getId());
        assertEquals("Assignment One", assignments.get(0).getName());
        assertEquals("2", assignments.get(1).getId());
        assertEquals("Assignment Two", assignments.get(1).getName());
        // the assignment's underlying Assignment object is merged in, not just the AssignmentExtended shell
        assertEquals("Assignment One", assignments.get(0).getAssignment().getName());
        assertEquals(Assignment.class, assignments.get(0).getType());
    }

    @Test
    public void testListCourseAssignmentsBuildsUrlWithCourseId() throws Exception {
        Response response = buildResponse(false, 200, "[]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assignmentExtended.listCourseAssignments(new ListCourseAssignmentsOptions("42"));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClient).sendApiGet(eq(oauthToken), urlCaptor.capture(), anyInt(), anyInt());
        assertTrue(urlCaptor.getValue().contains("courses/42/assignments"));
    }

    @Test
    public void testListCourseAssignmentsAcrossMultiplePagesAggregatesResults() throws Exception {
        Response pageOne = buildResponse(false, 200, "[{\"id\":1,\"name\":\"Page One\"}]", "https://canvas.example.com/api/v1/courses/1/assignments?page=2");
        Response pageTwo = buildResponse(false, 200, "[{\"id\":2,\"name\":\"Page Two\"}]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenReturn(pageOne)
            .thenReturn(pageTwo);

        List<AssignmentExtended> assignments = assignmentExtended.listCourseAssignments(new ListCourseAssignmentsOptions("1"));

        assertEquals(2, assignments.size());
        assertEquals("Page One", assignments.get(0).getName());
        assertEquals("Page Two", assignments.get(1).getName());
    }

    @Test
    public void testListCourseAssignmentsReturnsEmptyListOnCanvasError() throws Exception {
        Response response = buildResponse(true, 500, null, null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<AssignmentExtended> assignments = assignmentExtended.listCourseAssignments(new ListCourseAssignmentsOptions("1"));

        assertTrue(assignments.isEmpty());
    }

    @Test
    public void testListUserAssignmentsBuildsUrlWithUserAndCourseId() throws Exception {
        Response response = buildResponse(false, 200, "[{\"id\":3,\"name\":\"User Assignment\"}]", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<AssignmentExtended> assignments = assignmentExtended.listUserAssignments(new ListUserAssignmentOptions("1", "9"));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClient).sendApiGet(eq(oauthToken), urlCaptor.capture(), anyInt(), anyInt());
        assertTrue(urlCaptor.getValue().contains("users/9/courses/1/assignments"));
        assertEquals(1, assignments.size());
        assertEquals("User Assignment", assignments.get(0).getName());
    }

    @Test
    public void testGetSingleAssignmentReturnsParsedAssignment() throws Exception {
        Response response = buildResponse(false, 200, "{\"id\":5,\"name\":\"Quiz\"}", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        Optional<AssignmentExtended> result = assignmentExtended.getSingleAssignment(new GetSingleAssignmentOptions("1", 5L));

        assertTrue(result.isPresent());
        assertEquals("5", result.get().getId());
        assertEquals("Quiz", result.get().getName());
        assertEquals(Assignment.class, result.get().getType());
    }

    @Test
    public void testGetSingleAssignmentThrowsWhenCanvasReturnsErrorWithNoBody() throws Exception {
        // NOTE: unlike deleteAssignment, getSingleAssignment does not check response.getErrorHappened()/
        // getResponseCode() before parsing. When Canvas returns an error with an empty body, gson.fromJson
        // returns null and Optional.of(null) throws NPE instead of a handled empty Optional. Documenting
        // this existing behavior rather than fixing it.
        Response response = buildResponse(true, 404, null, null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(NullPointerException.class, () -> assignmentExtended.getSingleAssignment(new GetSingleAssignmentOptions("1", 999L)));
    }

    @Test
    public void testCreateAssignmentThrowsWhenNameIsBlank() {
        Assignment assignment = new Assignment();
        assignment.setName("  ");

        assertThrows(IllegalArgumentException.class, () -> assignmentExtended.createAssignment("1", assignment));
        verifyNoInteractions(restClient);
    }

    @Test
    public void testCreateAssignmentSuccessReturnsParsedAssignment() throws Exception {
        Response response = buildResponse(false, 200, "{\"id\":10,\"name\":\"New Assignment\"}", null);

        when(restClient.sendJsonPost(eq(oauthToken), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);

        Assignment assignment = new Assignment();
        assignment.setName("New Assignment");

        Optional<AssignmentExtended> result = assignmentExtended.createAssignment("1", assignment);

        assertTrue(result.isPresent());
        assertEquals("10", result.get().getId());
        assertEquals("New Assignment", result.get().getName());
    }

    @Test
    public void testDeleteAssignmentSuccessReturnsParsedAssignment() throws Exception {
        Response response = buildResponse(false, 200, "{\"id\":1,\"name\":\"To Delete\"}", null);

        when(restClient.sendApiDelete(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Optional<AssignmentExtended> result = assignmentExtended.deleteAssignment("1", 1L);

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
    }

    @Test
    public void testDeleteAssignmentReturnsEmptyOptionalOnCanvasError() throws Exception {
        Response response = buildResponse(true, 500, null, null);

        when(restClient.sendApiDelete(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Optional<AssignmentExtended> result = assignmentExtended.deleteAssignment("1", 1L);

        assertFalse(result.isPresent());
    }

    @Test
    public void testDeleteAssignmentReturnsEmptyOptionalOnNonTwoHundredResponseCode() throws Exception {
        Response response = buildResponse(false, 204, "{\"id\":1,\"name\":\"To Delete\"}", null);

        when(restClient.sendApiDelete(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Optional<AssignmentExtended> result = assignmentExtended.deleteAssignment("1", 1L);

        assertFalse(result.isPresent());
    }

    @Test
    public void testEditAssignmentSuccessReturnsParsedAssignment() throws Exception {
        Response response = buildResponse(false, 200, "{\"id\":7,\"name\":\"Edited\"}", null);

        when(restClient.sendJsonPut(eq(oauthToken), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);

        Assignment assignment = new Assignment();
        assignment.setId(7L);
        assignment.setName("Edited");

        Optional<AssignmentExtended> result = assignmentExtended.editAssignment("1", assignment);

        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("7", result.get().getId());
        assertEquals("Edited", result.get().getName());
        verify(restClient, times(1)).sendJsonPut(eq(oauthToken), anyString(), anyString(), anyInt(), anyInt());
    }

}
