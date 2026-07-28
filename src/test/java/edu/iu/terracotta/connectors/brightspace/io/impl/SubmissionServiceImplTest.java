package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public class SubmissionServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "12345";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private BrightspaceMessengerService brightspaceMessenger;
    @Mock private ResponseParserService responseParser;

    private ApiVersion apiVersion;
    private SubmissionServiceImpl submissionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        apiVersion = ApiVersion.builder().le("1.43").build();

        submissionService = new SubmissionServiceImpl(BASE_URL, apiVersion, oauthToken, restClient, 5000, 5000, 50, false);

        submissionService.brightspaceMessenger = brightspaceMessenger;
        submissionService.responseParser = responseParser;
    }

    @Test
    public void testListSubmissionsForMultipleAssignmentsAlwaysReturnsEmptyList() throws IOException {
        // NOTE: listSubmissionsForMultipleAssignments() is a stub that unconditionally returns
        // List.of() - it never touches orgUnitId, assignmentIds, the OAuth token, or the REST
        // client. Documented here as a known gap in the implementation, not fixed by this test.
        assertTrue(submissionService.listSubmissionsForMultipleAssignments(ORG_UNIT_ID, List.of()).isEmpty());
        assertTrue(submissionService.listSubmissionsForMultipleAssignments(ORG_UNIT_ID, List.of(1L, 2L, 3L)).isEmpty());
        assertTrue(submissionService.listSubmissionsForMultipleAssignments(null, null).isEmpty());
    }

    @Test
    public void testGetCourseSubmissionsAlwaysReturnsEmptyList() throws IOException {
        // NOTE: same stub situation as listSubmissionsForMultipleAssignments() above - the
        // orgUnitId argument is entirely ignored.
        assertTrue(submissionService.getCourseSubmissions(ORG_UNIT_ID).isEmpty());
        assertTrue(submissionService.getCourseSubmissions(null).isEmpty());
    }

    @Test
    public void testObjectTypeReturnsSubmissionExtended() {
        assertEquals(SubmissionExtended.class, submissionService.objectType());
    }

    @Test
    public void testListTypeReturnsParameterizedListOfSubmissionExtended() {
        ParameterizedType type = (ParameterizedType) submissionService.listType().getType();

        assertEquals(List.class, type.getRawType());
        assertEquals(SubmissionExtended.class, type.getActualTypeArguments()[0]);
    }

    @Test
    public void testConstructorWiresBaseServiceFields() {
        assertEquals(BASE_URL, submissionService.baseUrl);
        assertSame(apiVersion, submissionService.apiVersion);
        assertSame(oauthToken, submissionService.oauthToken);
        assertEquals(50, submissionService.paginationPageSize);
        assertEquals(Boolean.FALSE, submissionService.serializeNulls);
    }

    @Test
    public void testInheritedGetReturnsParsedObjectOnSuccess() throws IOException {
        Response response = Response.builder().responseCode(200).content("{}").build();
        SubmissionExtended submission = SubmissionExtended.builder().build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(response);
        when(responseParser.parseToObject(SubmissionExtended.class, response)).thenReturn(Optional.of(submission));

        Optional<SubmissionExtended> result = submissionService.get(BASE_URL + "/some/path");

        assertTrue(result.isPresent());
        assertSame(submission, result.get());
    }

    @Test
    public void testInheritedGetThrowsIOExceptionWhenErrorHappened() throws IOException {
        Response errorResponse = Response.builder().responseCode(200).errorHappened(true).build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(errorResponse);

        assertThrows(IOException.class, () -> submissionService.get(BASE_URL + "/some/path"));
    }

    @Test
    public void testInheritedGetThrowsIOExceptionWhenResponseCodeNot200() throws IOException {
        Response serverError = Response.builder().responseCode(500).build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(serverError);

        assertThrows(IOException.class, () -> submissionService.get(BASE_URL + "/some/path"));
    }

    @Test
    public void testInheritedGetListAggregatesResponsesAcrossPages() throws IOException {
        Response page1 = Response.builder().responseCode(200).content("page1").build();
        Response page2 = Response.builder().responseCode(200).content("page2").build();
        SubmissionExtended submission1 = SubmissionExtended.builder().build();
        SubmissionExtended submission2 = SubmissionExtended.builder().build();

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of(page1, page2));
        when(responseParser.<SubmissionExtended>parseToList(any(), eq(page1))).thenReturn(List.of(submission1));
        when(responseParser.<SubmissionExtended>parseToList(any(), eq(page2))).thenReturn(List.of(submission2));

        List<SubmissionExtended> result = submissionService.getList(BASE_URL + "/some/path");

        assertEquals(2, result.size());
        assertTrue(result.contains(submission1));
        assertTrue(result.contains(submission2));
    }

    @Test
    public void testInheritedGetListPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> submissionService.getList(BASE_URL + "/some/path"));
    }

}
