package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModuleUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

/**
 * The constructor wires up real BrightspaceMessengerServiceImpl/ResponseParserServiceImpl
 * instances; both are replaced with mocks (the fields are protected and this test lives in the
 * same package) so behavior of ContentObjectModuleServiceImpl itself - not its collaborators - is
 * what's under test.
 */
public class ContentObjectModuleServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "6";
    private static final long CONTENT_OBJECT_MODULE_ID = 9L;

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private ContentObjectModuleServiceImpl contentObjectModuleService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().le("1.30").lp("1.30").build();

        contentObjectModuleService = new ContentObjectModuleServiceImpl(
            BASE_URL,
            apiVersion,
            oauthToken,
            mock(RestClient.class),
            10,
            10,
            100,
            false
        );

        brightspaceMessenger = mock(BrightspaceMessengerService.class);
        responseParser = mock(ResponseParserService.class);
        contentObjectModuleService.brightspaceMessenger = brightspaceMessenger;
        contentObjectModuleService.responseParser = responseParser;
    }

    private String rootUrl() {
        return String.format("%s/d2l/api/le/%s/%s/content/root/", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID);
    }

    private String moduleUrl(long id) {
        return String.format("%s/d2l/api/le/%s/%s/content/modules/%s", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID, id);
    }

    private Response response(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    private ContentObjectModule module(long id, String title) {
        return ContentObjectModule.builder().id(id).title(title).build();
    }

    private ContentObjectModuleUpdate anyUpdate() {
        return ContentObjectModuleUpdate.builder().title("New Module").shortTitle("NM").build();
    }

    @Test
    public void testCreateSuccess() throws IOException {
        Response response = response("{}");
        ContentObjectModule created = module(1L, "New Module");

        when(brightspaceMessenger.sendJsonPost(eq(oauthToken), eq(rootUrl()), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.of(created));

        Optional<ContentObjectModule> result = contentObjectModuleService.create(ORG_UNIT_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(created, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPost(eq(oauthToken), eq(rootUrl()), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Module"));
    }

    @Test
    public void testCreateThrowsIOExceptionWhenResponseCannotBeParsed() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> contentObjectModuleService.create(ORG_UNIT_ID, anyUpdate()));
        assertEquals("Error creating Content Object Module", exception.getMessage());
    }

    @Test
    public void testCreatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenThrow(new IOException("network down"));

        assertThrows(IOException.class, () -> contentObjectModuleService.create(ORG_UNIT_ID, anyUpdate()));
    }

    @Test
    public void testGetAllForOrgUnitIdSuccessMergesResponses() throws IOException {
        Response page1 = response("page1");
        Response page2 = response("page2");
        ContentObjectModule module1 = module(1L, "Module 1");
        ContentObjectModule module2 = module(2L, "Module 2");
        ContentObjectModule module3 = module(3L, "Module 3");

        when(brightspaceMessenger.get(eq(oauthToken), eq(rootUrl()), isNull())).thenReturn(List.of(page1, page2));
        when(responseParser.<ContentObjectModule>parseToList(any(), eq(page1))).thenReturn(List.of(module1));
        when(responseParser.<ContentObjectModule>parseToList(any(), eq(page2))).thenReturn(List.of(module2, module3));

        List<ContentObjectModule> result = contentObjectModuleService.getAllForOrgUnitId(ORG_UNIT_ID);

        assertEquals(3, result.size());
        assertSame(module1, result.get(0));
        assertSame(module2, result.get(1));
        assertSame(module3, result.get(2));
    }

    @Test
    public void testGetAllForOrgUnitIdReturnsEmptyListWhenNoResponses() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), eq(rootUrl()), isNull())).thenReturn(List.of());

        List<ContentObjectModule> result = contentObjectModuleService.getAllForOrgUnitId(ORG_UNIT_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(responseParser, never()).parseToList(any(), any());
    }

    @Test
    public void testGetAllForOrgUnitIdPropagatesIOException() throws IOException {
        when(brightspaceMessenger.get(any(), any(), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> contentObjectModuleService.getAllForOrgUnitId(ORG_UNIT_ID));
    }

    @Test
    public void testGetSuccess() throws IOException {
        Response response = response("single");
        ContentObjectModule existing = module(CONTENT_OBJECT_MODULE_ID, "Existing Module");

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), eq(moduleUrl(CONTENT_OBJECT_MODULE_ID)))).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.of(existing));

        Optional<ContentObjectModule> result = contentObjectModuleService.get(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID);

        assertTrue(result.isPresent());
        assertSame(existing, result.get());
    }

    @Test
    public void testGetReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.getSingleResponse(any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.empty());

        Optional<ContentObjectModule> result = contentObjectModuleService.get(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.getSingleResponse(any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> contentObjectModuleService.get(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID));
    }

    @Test
    public void testUpdateSuccess() throws IOException {
        Response response = response("updated");
        ContentObjectModule updated = module(CONTENT_OBJECT_MODULE_ID, "Updated Module");

        when(brightspaceMessenger.sendJsonPut(eq(oauthToken), eq(moduleUrl(CONTENT_OBJECT_MODULE_ID)), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.of(updated));

        Optional<ContentObjectModule> result = contentObjectModuleService.update(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(updated, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPut(eq(oauthToken), eq(moduleUrl(CONTENT_OBJECT_MODULE_ID)), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Module"));
    }

    @Test
    public void testUpdateReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectModule.class, response)).thenReturn(Optional.empty());

        Optional<ContentObjectModule> result = contentObjectModuleService.update(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate());

        assertFalse(result.isPresent());
    }

    @Test
    public void testUpdatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> contentObjectModuleService.update(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate()));
    }

    @Test
    public void testDeleteSendsDeleteRequestToCorrectUrl() throws IOException {
        contentObjectModuleService.delete(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID);

        verify(brightspaceMessenger, times(1)).delete(eq(oauthToken), eq(moduleUrl(CONTENT_OBJECT_MODULE_ID)));
    }

    @Test
    public void testDeletePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.delete(any(), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> contentObjectModuleService.delete(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID));
    }

    @Test
    public void testListTypeAndObjectType() {
        assertNotNull(contentObjectModuleService.listType());
        assertEquals(ContentObjectModule.class, contentObjectModuleService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsField() {
        ContentObjectModuleReaderService result = contentObjectModuleService.withCallback(list -> { });

        assertSame(contentObjectModuleService, result);
        assertNotNull(contentObjectModuleService.responseCallback);
    }

}
