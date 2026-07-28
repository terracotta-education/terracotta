package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObjectUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public class GradeObjectServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "12345";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private BrightspaceMessengerService brightspaceMessenger;
    @Mock private ResponseParserService responseParser;

    private ApiVersion apiVersion;
    private GradeObjectServiceImpl gradeObjectService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        apiVersion = ApiVersion.builder().le("1.43").build();

        gradeObjectService = new GradeObjectServiceImpl(BASE_URL, apiVersion, oauthToken, restClient, 5000, 5000, 50, false);

        // replace the internal collaborators created inside BaseServiceImpl's constructor with mocks
        gradeObjectService.brightspaceMessenger = brightspaceMessenger;
        gradeObjectService.responseParser = responseParser;
    }

    private GradeObject gradeObject(long id) {
        return GradeObject.builder().id(id).name("grade-" + id).build();
    }

    private Response response(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    @Test
    public void testGetAllReturnsFlattenedList() throws IOException {
        Response page1 = response("page1");
        List<Response> responses = List.of(page1);
        List<GradeObject> parsed = List.of(gradeObject(1L), gradeObject(2L));

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(responses);
        when(responseParser.<GradeObject>parseToList(any(), eq(page1))).thenReturn(parsed);

        List<GradeObject> result = gradeObjectService.getAll(ORG_UNIT_ID);

        assertEquals(2, result.size());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).get(eq(oauthToken), urlCaptor.capture(), isNull());
        assertEquals(BASE_URL + "/d2l/api/le/1.43/" + ORG_UNIT_ID + "/grades/", urlCaptor.getValue());
    }

    @Test
    public void testGetAllReturnsEmptyListWhenNoResponses() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of());

        List<GradeObject> result = gradeObjectService.getAll(ORG_UNIT_ID);

        assertTrue(result.isEmpty());
        verify(responseParser, never()).parseToList(any(), any());
    }

    @Test
    public void testGetAllPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> gradeObjectService.getAll(ORG_UNIT_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllWithCallbackInvokesRegisteredConsumer() throws IOException {
        Response page1 = response("page1");
        List<GradeObject> parsed = List.of(gradeObject(5L));

        when(responseParser.<GradeObject>parseToList(any(), eq(page1))).thenReturn(parsed);

        // BrightspaceMessengerServiceImpl invokes the per-page callback synchronously, on each
        // page, before the aggregated list of responses is returned to the caller. Simulate that
        // here via thenAnswer rather than invoking the captured consumer after the stubbed call
        // returns, since the real "responseCallback" field is reset to null immediately after
        // getListResponse()'s call to brightspaceMessenger.get() completes.
        when(brightspaceMessenger.get(eq(oauthToken), any(), any())).thenAnswer(
            invocation -> {
                Consumer<Response> consumer = invocation.getArgument(2);

                if (consumer != null) {
                    consumer.accept(page1);
                }

                return List.of(page1);
            }
        );

        Consumer<List<GradeObject>> externalCallback = mock(Consumer.class);
        gradeObjectService.withCallback(externalCallback);

        gradeObjectService.getAll(ORG_UNIT_ID);

        verify(externalCallback).accept(parsed);
    }

    @Test
    public void testGetLatestReturnsHighestGradeObjectId() throws IOException {
        Response page1 = response("page1");
        List<GradeObject> parsed = List.of(gradeObject(1L), gradeObject(3L), gradeObject(2L));

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of(page1));
        when(responseParser.<GradeObject>parseToList(any(), eq(page1))).thenReturn(parsed);

        Optional<GradeObject> result = gradeObjectService.getLatest(ORG_UNIT_ID);

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().getId());
    }

    @Test
    public void testGetLatestReturnsEmptyWhenNoGradeObjects() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of());

        Optional<GradeObject> result = gradeObjectService.getLatest(ORG_UNIT_ID);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetReturnsParsedGradeObject() throws IOException {
        long gradeObjectId = 99L;
        Response response = response("single");
        GradeObject gradeObject = gradeObject(gradeObjectId);

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(response);
        when(responseParser.parseToObject(GradeObject.class, response)).thenReturn(Optional.of(gradeObject));

        Optional<GradeObject> result = gradeObjectService.get(ORG_UNIT_ID, gradeObjectId);

        assertTrue(result.isPresent());
        assertEquals(gradeObjectId, result.get().getId());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).getSingleResponse(eq(oauthToken), urlCaptor.capture());
        assertEquals(BASE_URL + "/d2l/api/le/1.43/" + ORG_UNIT_ID + "/grades/" + gradeObjectId, urlCaptor.getValue());
    }

    @Test
    public void testGetReturnsEmptyWhenParserReturnsEmpty() throws IOException {
        Response response = response("single");

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(response);
        when(responseParser.parseToObject(GradeObject.class, response)).thenReturn(Optional.empty());

        Optional<GradeObject> result = gradeObjectService.get(ORG_UNIT_ID, 99L);

        assertFalse(result.isPresent());
    }

    @Test
    public void testUpdateSendsJsonPutAndReturnsParsedResult() throws IOException {
        long gradeObjectId = 42L;
        GradeObjectUpdate update = GradeObjectUpdate.builder().name("Updated Name").build();
        Response response = response("updated");
        GradeObject updated = gradeObject(gradeObjectId);

        when(brightspaceMessenger.sendJsonPut(eq(oauthToken), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(GradeObject.class, response)).thenReturn(Optional.of(updated));

        Optional<GradeObject> result = gradeObjectService.update(ORG_UNIT_ID, gradeObjectId, update);

        assertTrue(result.isPresent());
        assertEquals(gradeObjectId, result.get().getId());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPut(eq(oauthToken), urlCaptor.capture(), bodyCaptor.capture());
        assertEquals(BASE_URL + "/d2l/api/le/1.43/" + ORG_UNIT_ID + "/grades/" + gradeObjectId, urlCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains("Updated Name"));
    }

    @Test
    public void testDeleteInvokesMessengerWithExpectedUrl() throws IOException {
        long gradeObjectId = 7L;

        gradeObjectService.delete(ORG_UNIT_ID, gradeObjectId);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger, times(1)).delete(eq(oauthToken), urlCaptor.capture());
        assertEquals(BASE_URL + "/d2l/api/le/1.43/" + ORG_UNIT_ID + "/grades/" + gradeObjectId, urlCaptor.getValue());
    }

    @Test
    public void testDeletePropagatesIOException() throws IOException {
        when(brightspaceMessenger.delete(eq(oauthToken), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> gradeObjectService.delete(ORG_UNIT_ID, 7L));
    }

    @Test
    public void testListTypeAndObjectType() {
        assertEquals(GradeObject.class, gradeObjectService.objectType());
        assertTrue(gradeObjectService.listType() != null);
    }

}
