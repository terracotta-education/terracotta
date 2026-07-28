package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FolderExtended;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;

public class FolderExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private FolderExtendedImpl folderExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        folderExtended = new FolderExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    @Test
    public void testGetFolderReturnsParsedFolder() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("{\"id\":\"1\",\"name\":\"conversation attachments\",\"full_name\":\"my files/conversation attachments\",\"files_url\":\"https://canvas.example.com/api/v1/folders/1/files\"}");

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        Optional<FolderExtended> result = folderExtended.getFolder("1");

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
        assertEquals("conversation attachments", result.get().getName());
        assertEquals("my files/conversation attachments", result.get().getFullName());
        assertEquals("https://canvas.example.com/api/v1/folders/1/files", result.get().getFilesUrl());
    }

    @Test
    public void testGetFolderDoesNotCheckErrorFlagAndReturnsParsedContentRegardless() throws Exception {
        // Unlike FileExtendedImpl.getFile(), FolderExtendedImpl.getFolder() never checks
        // response.getErrorHappened() / getResponseCode() before parsing - it happily parses
        // whatever content came back even when Canvas signalled an error.
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(500);
        response.setContent("{\"id\":\"1\",\"name\":\"conversation attachments\"}");

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        Optional<FolderExtended> result = folderExtended.getFolder("1");

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
    }

    @Test
    public void testGetFolderThrowsNullPointerExceptionOnEmptyErrorResponseBody() throws Exception {
        // Real-world Canvas error responses (404/500) typically have no body. Because getFolder()
        // skips the error check that getFile() performs, it hands a null body straight to Gson,
        // which returns null, and then does Optional.of(null) - throwing an unchecked NPE instead
        // of a clean, catchable IOException.
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(404);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(NullPointerException.class, () -> folderExtended.getFolder("999"));
    }

    @Test
    public void testGetFoldersParsesAllFoldersInSinglePage() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[{\"id\":\"1\",\"name\":\"folder-a\"},{\"id\":\"2\",\"name\":\"folder-b\"}]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FolderExtended> results = folderExtended.getFolders();

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("folder-a", results.get(0).getName());
        assertEquals("2", results.get(1).getId());
        assertEquals("folder-b", results.get(1).getName());
    }

    @Test
    public void testGetFoldersAggregatesResultsAcrossMultiplePages() throws Exception {
        Response pageOne = new Response();
        pageOne.setErrorHappened(false);
        pageOne.setResponseCode(200);
        pageOne.setContent("[{\"id\":\"1\",\"name\":\"folder-a\"}]");
        pageOne.setNextLink("https://canvas.example.com/api/v1/users/self/folders/by_path/conversation%20attachments?page=2");

        Response pageTwo = new Response();
        pageTwo.setErrorHappened(false);
        pageTwo.setResponseCode(200);
        pageTwo.setContent("[{\"id\":\"2\",\"name\":\"folder-b\"}]");
        pageTwo.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenReturn(pageOne)
            .thenReturn(pageTwo);

        List<FolderExtended> results = folderExtended.getFolders();

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("2", results.get(1).getId());
    }

    @Test
    public void testGetFoldersReturnsEmptyListWhenCanvasHasNoFolders() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FolderExtended> results = folderExtended.getFolders();

        assertTrue(results.isEmpty());
    }

    @Test
    public void testGetFoldersReturnsEmptyListWhenCanvasErrors() throws Exception {
        // getFolders() goes through canvasMessenger.getFromCanvas(), which (unlike getFolder())
        // does check the error flag and silently returns an empty list of responses.
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(500);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FolderExtended> results = folderExtended.getFolders();

        assertTrue(results.isEmpty());
    }

}
