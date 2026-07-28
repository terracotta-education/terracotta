package edu.iu.terracotta.connectors.brightspace.io.net.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.config.SpringContext;
import edu.iu.terracotta.connectors.brightspace.configuration.BrightspaceConfigurationService;
import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

/**
 * {@link RefreshingRestClient#restClient} is constructed inline ({@code new SimpleRestClient()})
 * rather than injected, so the real delegate is swapped for a mock via {@code ReflectionTestUtils}
 * after construction -- the same convention used elsewhere in this suite for internally-constructed
 * (non-DI) collaborator fields.
 */
@ExtendWith(MockitoExtension.class)
public class RefreshingRestClientTest {

    private static final String URL = "https://example.com/api";
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;

    @Mock private RestClient restClient;
    @Mock private OauthToken token;
    @Mock private Map<String, java.util.List<String>> params;
    @Mock private InputStream inputStream;

    private RefreshingRestClient refreshingRestClient;

    @BeforeEach
    void beforeEach() {
        // RefreshingRestClient's field initializer constructs a real SimpleRestClient(), which
        // reaches into SpringContext.getBean(...) - stub the static Spring context so that
        // no-arg construction doesn't NPE outside a container, before swapping in the mock delegate
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        BrightspaceConfigurationService brightspaceConfigurationService = mock(BrightspaceConfigurationService.class);
        when(applicationContext.getBean(BrightspaceConfigurationService.class)).thenReturn(brightspaceConfigurationService);
        ReflectionTestUtils.setField(SpringContext.class, "context", applicationContext);

        refreshingRestClient = new RefreshingRestClient();
        ReflectionTestUtils.setField(refreshingRestClient, "restClient", restClient);
    }

    @Test
    void testSendApiGet() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendApiGet(any(), anyString(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendApiGet(token, URL, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendApiGetRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendApiGet(any(), anyString(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendApiGet(token, URL, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiGet(any(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSendApiGetThrowsWhenRetryAlsoFails() throws IOException {
        when(restClient.sendApiGet(any(), anyString(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException());

        assertThrows(
            InvalidOauthTokenException.class,
            () -> refreshingRestClient.sendApiGet(token, URL, CONNECT_TIMEOUT, READ_TIMEOUT)
        );

        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiGet(any(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSendJsonPost() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendJsonPost(any(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendJsonPost(token, URL, "{}", CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendJsonPostRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendJsonPost(any(), anyString(), anyString(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendJsonPost(token, URL, "{}", CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendJsonPost(any(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSendJsonPut() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendJsonPut(any(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendJsonPut(token, URL, "{}", CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendJsonPutRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendJsonPut(any(), anyString(), anyString(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendJsonPut(token, URL, "{}", CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendJsonPut(any(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSendApiPost() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendApiPost(any(), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendApiPost(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendApiPostRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendApiPost(any(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendApiPost(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiPost(any(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void testSendApiPostFile() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendApiPostFile(any(), anyString(), any(), anyString(), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendApiPostFile(token, URL, params, "file", "/tmp/file", inputStream, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendApiPostFileRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(201).build();
        when(restClient.sendApiPostFile(any(), anyString(), any(), anyString(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendApiPostFile(token, URL, params, "file", "/tmp/file", inputStream, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiPostFile(any(), anyString(), any(), anyString(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void testSendApiDelete() throws IOException {
        Response response = Response.builder().responseCode(204).build();
        when(restClient.sendApiDelete(any(), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendApiDelete(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendApiDeleteRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(204).build();
        when(restClient.sendApiDelete(any(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendApiDelete(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiDelete(any(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void testSendApiPut() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendApiPut(any(), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        Response result = refreshingRestClient.sendApiPut(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, never()).refresh();
    }

    @Test
    void testSendApiPutRetriesAfterTokenRefresh() throws IOException {
        Response response = Response.builder().responseCode(200).build();
        when(restClient.sendApiPut(any(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException())
            .thenReturn(response);

        Response result = refreshingRestClient.sendApiPut(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT);

        assertSame(response, result);
        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiPut(any(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void testSendApiPutThrowsWhenRetryAlsoFails() throws IOException {
        when(restClient.sendApiPut(any(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new InvalidOauthTokenException());

        assertThrows(
            InvalidOauthTokenException.class,
            () -> refreshingRestClient.sendApiPut(token, URL, params, CONNECT_TIMEOUT, READ_TIMEOUT)
        );

        verify(token, times(1)).refresh();
        verify(restClient, times(2)).sendApiPut(any(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void testSendUpload() throws IOException {
        when(restClient.sendUpload(anyString(), any(), any(), anyString(), anyInt(), anyInt())).thenReturn("uploadResult");

        String result = refreshingRestClient.sendUpload(URL, params, inputStream, "filename.txt", CONNECT_TIMEOUT, READ_TIMEOUT);

        assertEquals("uploadResult", result);
        verify(token, never()).refresh();
    }

}
