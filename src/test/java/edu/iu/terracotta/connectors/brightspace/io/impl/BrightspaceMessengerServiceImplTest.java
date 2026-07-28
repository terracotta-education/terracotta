package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public class BrightspaceMessengerServiceImplTest {

    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;
    private static final String URL = "https://brightspace.example.com/d2l/api/le/1.43/12345/grades/";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private BrightspaceMessengerServiceImpl brightspaceMessengerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        brightspaceMessengerService = new BrightspaceMessengerServiceImpl(CONNECT_TIMEOUT, READ_TIMEOUT, restClient);
    }

    private Response okResponse(String next) {
        return Response.builder().responseCode(200).content("body").next(next).build();
    }

    @Test
    public void testGetSingleResponseDelegatesToRestClient() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.getSingleResponse(oauthToken, URL);

        assertSame(response, result);
    }

    @Test
    public void testGetWithoutCallbackReturnsSinglePageWhenNoNextUrl() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL);

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
        verify(restClient, times(1)).sendApiGet(eq(oauthToken), eq(URL), eq(CONNECT_TIMEOUT), eq(READ_TIMEOUT));
    }

    @Test
    public void testGetFollowsNextUrlUntilExhausted() throws IOException {
        String url2 = URL + "?bookmark=2";
        Response page1 = okResponse(url2);
        Response page2 = okResponse(null);

        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page1);
        when(restClient.sendApiGet(oauthToken, url2, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page2);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL, null);

        assertEquals(2, result.size());
        assertSame(page1, result.get(0));
        assertSame(page2, result.get(1));
        verify(restClient, times(1)).sendApiGet(eq(oauthToken), eq(URL), eq(CONNECT_TIMEOUT), eq(READ_TIMEOUT));
        verify(restClient, times(1)).sendApiGet(eq(oauthToken), eq(url2), eq(CONNECT_TIMEOUT), eq(READ_TIMEOUT));
    }

    @Test
    public void testGetReturnsEmptyListWhenErrorHappened() throws IOException {
        Response errorResponse = Response.builder().responseCode(200).errorHappened(true).build();
        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(errorResponse);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetReturnsEmptyListWhenResponseCodeIsNot200() throws IOException {
        Response notFound = Response.builder().responseCode(404).build();
        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(notFound);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetStopsPaginatingAsSoonAsAPageErrors() throws IOException {
        String url2 = URL + "?bookmark=2";
        Response page1 = okResponse(url2);
        Response page2Error = Response.builder().responseCode(500).build();

        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page1);
        when(restClient.sendApiGet(oauthToken, url2, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page2Error);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL);

        // the entire aggregated result is discarded (not just the failing page) once any page errors
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetInvokesCallbackForEachSuccessfulPage() throws IOException {
        String url2 = URL + "?bookmark=2";
        Response page1 = okResponse(url2);
        Response page2 = okResponse(null);

        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page1);
        when(restClient.sendApiGet(oauthToken, url2, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(page2);

        Consumer<Response> callback = mock(Consumer.class);

        List<Response> result = brightspaceMessengerService.get(oauthToken, URL, callback);

        assertEquals(2, result.size());
        verify(callback, times(1)).accept(page1);
        verify(callback, times(1)).accept(page2);
    }

    @Test
    public void testGetPropagatesIOExceptionFromRestClient() throws IOException {
        when(restClient.sendApiGet(oauthToken, URL, CONNECT_TIMEOUT, READ_TIMEOUT)).thenThrow(new IOException("network error"));

        assertThrows(
            IOException.class,
            () -> brightspaceMessengerService.get(oauthToken, URL)
        );
    }

    @Test
    public void testSendWithoutParametersDelegatesWithEmptyMap() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendApiPost(oauthToken, URL, Map.of(), CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.send(oauthToken, URL);

        assertSame(response, result);
        verify(restClient).sendApiPost(eq(oauthToken), eq(URL), eq(Map.of()), eq(CONNECT_TIMEOUT), eq(READ_TIMEOUT));
    }

    @Test
    public void testSendWithParametersDelegatesToRestClient() throws IOException {
        Map<String, List<String>> parameters = Map.of("key", List.of("value"));
        Response response = okResponse(null);
        when(restClient.sendApiPost(oauthToken, URL, parameters, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.send(oauthToken, URL, parameters);

        assertSame(response, result);
    }

    @Test
    public void testSendFileWithoutParametersDelegatesWithEmptyMap() throws IOException {
        InputStream is = new ByteArrayInputStream("data".getBytes());
        Response response = okResponse(null);
        when(restClient.sendApiPostFile(oauthToken, URL, Map.of(), "file", "path.txt", is, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.sendFile(oauthToken, URL, "file", "path.txt", is);

        assertSame(response, result);
    }

    @Test
    public void testSendFileWithParametersDelegatesToRestClient() throws IOException {
        InputStream is = new ByteArrayInputStream("data".getBytes());
        Map<String, List<String>> parameters = Map.of("key", List.of("value"));
        Response response = okResponse(null);
        when(restClient.sendApiPostFile(oauthToken, URL, parameters, "file", "path.txt", is, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.sendFile(oauthToken, URL, parameters, "file", "path.txt", is);

        assertSame(response, result);
    }

    @Test
    public void testSendJsonPostDelegatesToRestClient() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendJsonPost(oauthToken, URL, "{\"a\":1}", CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.sendJsonPost(oauthToken, URL, "{\"a\":1}");

        assertSame(response, result);
    }

    @Test
    public void testSendJsonPutDelegatesToRestClient() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendJsonPut(oauthToken, URL, "{\"a\":1}", CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.sendJsonPut(oauthToken, URL, "{\"a\":1}");

        assertSame(response, result);
    }

    @Test
    public void testDeleteWithoutParametersDelegatesWithEmptyMap() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendApiDelete(oauthToken, URL, Map.of(), CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.delete(oauthToken, URL);

        assertSame(response, result);
    }

    @Test
    public void testDeleteWithParametersDelegatesToRestClient() throws IOException {
        Map<String, List<String>> parameters = Map.of("key", List.of("value"));
        Response response = okResponse(null);
        when(restClient.sendApiDelete(oauthToken, URL, parameters, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.delete(oauthToken, URL, parameters);

        assertSame(response, result);
    }

    @Test
    public void testPutWithoutParametersDelegatesWithEmptyMap() throws IOException {
        Response response = okResponse(null);
        when(restClient.sendApiPut(oauthToken, URL, Map.of(), CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.put(oauthToken, URL);

        assertSame(response, result);
    }

    @Test
    public void testPutWithParametersDelegatesToRestClient() throws IOException {
        Map<String, List<String>> parameters = Map.of("key", List.of("value"));
        Response response = okResponse(null);
        when(restClient.sendApiPut(oauthToken, URL, parameters, CONNECT_TIMEOUT, READ_TIMEOUT)).thenReturn(response);

        Response result = brightspaceMessengerService.put(oauthToken, URL, parameters);

        assertSame(response, result);
    }

    @Test
    public void testGetNeverCallsRestClientWhenUrlBlank() throws IOException {
        List<Response> result = brightspaceMessengerService.get(oauthToken, "");

        assertTrue(result.isEmpty());
        verify(restClient, never()).sendApiGet(any(), any(), anyInt(), anyInt());
    }

}
