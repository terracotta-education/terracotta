package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FileExtended;
import edu.ksu.canvas.model.File;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;

public class FileExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private FileExtendedImpl fileExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        fileExtended = new FileExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    @Test
    public void testGetFileReturnsParsedFile() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("{\"id\":5,\"display_name\":\"single.txt\",\"filename\":\"single.txt\",\"url\":\"https://canvas.example.com/files/5\",\"size\":123}");

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        Optional<FileExtended> result = fileExtended.getFile("https://canvas.example.com/files/5");

        assertTrue(result.isPresent());
        assertEquals("5", result.get().getId());
        assertEquals("single.txt", result.get().getDisplayName());
        assertEquals("https://canvas.example.com/files/5", result.get().getUrl());
        assertEquals(123L, result.get().getSize());
        assertEquals(File.class, result.get().getType());
    }

    @Test
    public void testGetFileThrowsIOExceptionWhenCanvasReturnsError() throws Exception {
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(404);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(IOException.class, () -> fileExtended.getFile("https://canvas.example.com/files/999"));
    }

    @Test
    public void testGetFileThrowsIOExceptionWhenResponseCodeIsNot200() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(500);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(IOException.class, () -> fileExtended.getFile("https://canvas.example.com/files/999"));
    }

    @Test
    public void testGetFilesParsesAllFilesInSinglePage() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[{\"id\":1,\"display_name\":\"a.txt\"},{\"id\":2,\"display_name\":\"b.txt\"}]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FileExtended> results = fileExtended.getFiles("courses/1/files");

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("a.txt", results.get(0).getDisplayName());
        assertEquals(File.class, results.get(0).getType());
        assertEquals("2", results.get(1).getId());
        assertEquals("b.txt", results.get(1).getDisplayName());
    }

    @Test
    public void testGetFilesAggregatesResultsAcrossMultiplePages() throws Exception {
        Response pageOne = new Response();
        pageOne.setErrorHappened(false);
        pageOne.setResponseCode(200);
        pageOne.setContent("[{\"id\":1,\"display_name\":\"a.txt\"}]");
        pageOne.setNextLink("https://canvas.example.com/api/v1/courses/1/files?page=2");

        Response pageTwo = new Response();
        pageTwo.setErrorHappened(false);
        pageTwo.setResponseCode(200);
        pageTwo.setContent("[{\"id\":2,\"display_name\":\"b.txt\"}]");
        pageTwo.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt()))
            .thenReturn(pageOne)
            .thenReturn(pageTwo);

        List<FileExtended> results = fileExtended.getFiles("courses/1/files");

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("2", results.get(1).getId());
    }

    @Test
    public void testGetFilesReturnsEmptyListWhenCanvasHasNoFiles() throws Exception {
        Response response = new Response();
        response.setErrorHappened(false);
        response.setResponseCode(200);
        response.setContent("[]");
        response.setNextLink(null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FileExtended> results = fileExtended.getFiles("courses/1/files");

        assertTrue(results.isEmpty());
    }

    @Test
    public void testGetFilesReturnsEmptyListWhenCanvasErrors() throws Exception {
        Response response = new Response();
        response.setErrorHappened(true);
        response.setResponseCode(500);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        List<FileExtended> results = fileExtended.getFiles("courses/1/files");

        assertTrue(results.isEmpty());
    }

}
