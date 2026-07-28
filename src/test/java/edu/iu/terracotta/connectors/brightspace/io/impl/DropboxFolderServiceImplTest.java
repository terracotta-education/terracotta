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
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolderUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

/**
 * The constructor wires up real BrightspaceMessengerServiceImpl/ResponseParserServiceImpl
 * instances; both are replaced with mocks (the fields are protected and this test lives in the
 * same package) so behavior of DropboxFolderServiceImpl itself - not its collaborators - is what's
 * under test.
 */
public class DropboxFolderServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "6";
    private static final long DROPBOX_FOLDER_ID = 9L;

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private DropboxFolderServiceImpl dropboxFolderService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().le("1.30").lp("1.30").build();

        dropboxFolderService = new DropboxFolderServiceImpl(
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
        dropboxFolderService.brightspaceMessenger = brightspaceMessenger;
        dropboxFolderService.responseParser = responseParser;
    }

    private String rootUrl() {
        return String.format("%s/d2l/api/le/%s/%s/dropbox/folders/", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID);
    }

    private String folderUrl(long id) {
        return String.format("%s/d2l/api/le/%s/%s/dropbox/folders/%s", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID, id);
    }

    private Response response(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    private DropboxFolder folder(long id, String name) {
        return DropboxFolder.builder().id(id).name(name).build();
    }

    private DropboxFolderUpdate anyUpdate() {
        return DropboxFolderUpdate.builder().name("New Folder").build();
    }

    @Test
    public void testCreateSuccess() throws IOException {
        Response response = response("{}");
        DropboxFolder created = folder(1L, "New Folder");

        when(brightspaceMessenger.sendJsonPost(eq(oauthToken), eq(rootUrl()), any())).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.of(created));

        Optional<DropboxFolder> result = dropboxFolderService.create(ORG_UNIT_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(created, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPost(eq(oauthToken), eq(rootUrl()), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Folder"));
    }

    @Test
    public void testCreateThrowsIOExceptionWhenResponseCannotBeParsed() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> dropboxFolderService.create(ORG_UNIT_ID, anyUpdate()));
        assertEquals("Error creating DropBox Folder", exception.getMessage());
    }

    @Test
    public void testCreatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenThrow(new IOException("network down"));

        assertThrows(IOException.class, () -> dropboxFolderService.create(ORG_UNIT_ID, anyUpdate()));
    }

    @Test
    public void testGetAllForOrgUnitIdSuccessMergesResponses() throws IOException {
        Response page1 = response("page1");
        Response page2 = response("page2");
        DropboxFolder folder1 = folder(1L, "Folder 1");
        DropboxFolder folder2 = folder(2L, "Folder 2");
        DropboxFolder folder3 = folder(3L, "Folder 3");

        when(brightspaceMessenger.get(eq(oauthToken), eq(rootUrl()), isNull())).thenReturn(List.of(page1, page2));
        when(responseParser.<DropboxFolder>parseToList(any(), eq(page1))).thenReturn(List.of(folder1));
        when(responseParser.<DropboxFolder>parseToList(any(), eq(page2))).thenReturn(List.of(folder2, folder3));

        List<DropboxFolder> result = dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID);

        assertEquals(3, result.size());
        assertSame(folder1, result.get(0));
        assertSame(folder2, result.get(1));
        assertSame(folder3, result.get(2));
    }

    @Test
    public void testGetAllForOrgUnitIdReturnsEmptyListWhenNoResponses() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), eq(rootUrl()), isNull())).thenReturn(List.of());

        List<DropboxFolder> result = dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(responseParser, never()).parseToList(any(), any());
    }

    @Test
    public void testGetAllForOrgUnitIdPropagatesIOException() throws IOException {
        when(brightspaceMessenger.get(any(), any(), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> dropboxFolderService.getAllForOrgUnitId(ORG_UNIT_ID));
    }

    @Test
    public void testGetSuccess() throws IOException {
        Response response = response("single");
        DropboxFolder existing = folder(DROPBOX_FOLDER_ID, "Existing Folder");

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), eq(folderUrl(DROPBOX_FOLDER_ID)))).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.of(existing));

        Optional<DropboxFolder> result = dropboxFolderService.get(ORG_UNIT_ID, DROPBOX_FOLDER_ID);

        assertTrue(result.isPresent());
        assertSame(existing, result.get());
    }

    @Test
    public void testGetReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.getSingleResponse(any(), any())).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.empty());

        Optional<DropboxFolder> result = dropboxFolderService.get(ORG_UNIT_ID, DROPBOX_FOLDER_ID);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.getSingleResponse(any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> dropboxFolderService.get(ORG_UNIT_ID, DROPBOX_FOLDER_ID));
    }

    @Test
    public void testUpdateSuccess() throws IOException {
        Response response = response("updated");
        DropboxFolder updated = folder(DROPBOX_FOLDER_ID, "Updated Folder");

        when(brightspaceMessenger.sendJsonPut(eq(oauthToken), eq(folderUrl(DROPBOX_FOLDER_ID)), any())).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.of(updated));

        Optional<DropboxFolder> result = dropboxFolderService.update(ORG_UNIT_ID, DROPBOX_FOLDER_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(updated, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPut(eq(oauthToken), eq(folderUrl(DROPBOX_FOLDER_ID)), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Folder"));
    }

    @Test
    public void testUpdateReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(DropboxFolder.class, response)).thenReturn(Optional.empty());

        Optional<DropboxFolder> result = dropboxFolderService.update(ORG_UNIT_ID, DROPBOX_FOLDER_ID, anyUpdate());

        assertFalse(result.isPresent());
    }

    @Test
    public void testUpdatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> dropboxFolderService.update(ORG_UNIT_ID, DROPBOX_FOLDER_ID, anyUpdate()));
    }

    @Test
    public void testDeleteSendsDeleteRequestToCorrectUrl() throws IOException {
        dropboxFolderService.delete(ORG_UNIT_ID, DROPBOX_FOLDER_ID);

        verify(brightspaceMessenger, times(1)).delete(eq(oauthToken), eq(folderUrl(DROPBOX_FOLDER_ID)));
    }

    @Test
    public void testDeletePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.delete(any(), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> dropboxFolderService.delete(ORG_UNIT_ID, DROPBOX_FOLDER_ID));
    }

    @Test
    public void testListTypeAndObjectType() {
        assertNotNull(dropboxFolderService.listType());
        assertEquals(DropboxFolder.class, dropboxFolderService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsField() {
        DropboxFolderReaderService result = dropboxFolderService.withCallback(list -> { });

        assertSame(dropboxFolderService, result);
        assertNotNull(dropboxFolderService.responseCallback);
    }

}
