package edu.iu.terracotta.connectors.brightspace.io.net.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import edu.iu.terracotta.connectors.brightspace.io.exception.BrightspaceException;
import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.ksu.canvas.exception.RateLimitException;

class SimpleRestClientTest {

    private static final String URL = "https://brightspace.example.edu/d2l/api/resource";

    private final SimpleRestClient simpleRestClient = new SimpleRestClient(false);

    private OauthToken token() {
        OauthToken token = mock(OauthToken.class);
        when(token.getAccessToken()).thenReturn("access-token");
        return token;
    }

    private StatusLine statusLine(int code) {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(code);
        when(statusLine.getReasonPhrase()).thenReturn("reason-" + code);
        return statusLine;
    }

    private CloseableHttpResponse httpResponse(int statusCode) {
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = statusLine(statusCode);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        return httpResponse;
    }

    private CloseableHttpClient mockClientReturning(CloseableHttpResponse response) throws IOException {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);

        return httpClient;
    }

    // Production code funnels every request through HttpClientBuilder.create()...build(), which
    // would otherwise open a real socket. Intercepting the static factory (same idiom used
    // elsewhere in this suite for other static factories, e.g. mockStatic(SesClient.class) in
    // MessageEmailServiceImplTest) lets us hand back a fully-mocked client instead.
    private MockedStatic<HttpClientBuilder> mockHttpClientBuilder(CloseableHttpClient httpClient) {
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        when(builder.setDefaultRequestConfig(any())).thenReturn(builder);
        when(builder.disableRedirectHandling()).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

        MockedStatic<HttpClientBuilder> mockedStatic = mockStatic(HttpClientBuilder.class);
        mockedStatic.when(HttpClientBuilder::create).thenReturn(builder);

        return mockedStatic;
    }

    private Map<String, List<String>> sampleParams() {
        Map<String, List<String>> params = new HashMap<>();
        params.put("name", List.of("value1", "value2"));
        params.put("emptyKey", List.of());

        return params;
    }

    /* sendApiGet */

    @Test
    void testSendApiGetHappyPathNoLinkHeaderReturnsEarly() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiGet(token(), URL, 100, 100);

            assertEquals(200, result.getResponseCode());
            assertEquals("{\"id\":1}", result.getContent());
            assertNull(result.getNext());
        }
    }

    @Test
    void testSendApiGetSetsNextFromLinkHeader() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("[]", ContentType.APPLICATION_JSON));
        when(response.getFirstHeader("Link")).thenReturn(new BasicHeader("Link", "<https://brightspace.example.edu/next-page>; rel=\"next\""));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiGet(token(), URL, 100, 100);

            assertEquals("https://brightspace.example.edu/next-page", result.getNext());
        }
    }

    @Test
    void testSendApiGetNotFoundSwallowsExceptionAndReturnsNullContent() throws Exception {
        CloseableHttpResponse response = httpResponse(404);
        when(response.getEntity()).thenReturn(new StringEntity("not found", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiGet(token(), URL, 100, 100);

            assertEquals(404, result.getResponseCode());
            assertNull(result.getContent());
        }
    }

    @Test
    void testSendApiGet401WithWwwAuthenticateThrowsInvalidOauthTokenException() throws Exception {
        CloseableHttpResponse response = httpResponse(401);
        when(response.containsHeader(HttpHeaders.WWW_AUTHENTICATE)).thenReturn(true);
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            assertThrows(InvalidOauthTokenException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));
        }
    }

    @Test
    void testSendApiGet401WithoutWwwAuthenticateThrowsBrightspaceException() throws Exception {
        CloseableHttpResponse response = httpResponse(401);
        when(response.containsHeader(HttpHeaders.WWW_AUTHENTICATE)).thenReturn(false);
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            BrightspaceException exception = assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));

            assertEquals("User is not authorized to perform this action", exception.getBrightspaceErrorMessage());
        }
    }

    @Test
    void testSendApiGet429ThrowsBrightspaceException() throws Exception {
        CloseableHttpResponse response = httpResponse(429);
        when(response.getEntity()).thenReturn(new StringEntity("throttled", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));
        }
    }

    @Test
    void testSendApiGet504ThrowsBrightspaceException() throws Exception {
        CloseableHttpResponse response = httpResponse(504);
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            BrightspaceException exception = assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));

            assertTrue(exception.getMessage().contains("Gateway Time-out"));
        }
    }

    @Test
    void testSendApiGetGenericBadStatusThrowsBrightspaceExceptionViaHandleError() throws Exception {
        CloseableHttpResponse response = httpResponse(400);
        when(response.getEntity()).thenReturn(new StringEntity("bad request", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));
        }
    }

    @Test
    void testSendApiGetRateLimitExceededThrowsRateLimitException() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getFirstHeader("X-Rate-Limit-Remaining")).thenReturn(new BasicHeader("X-Rate-Limit-Remaining", "0.05"));
        when(response.getEntity()).thenReturn(new StringEntity("limited", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            assertThrows(RateLimitException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));
        }
    }

    /* extractErrorMessageFromResponse (exercised via the 403 checkHeaders branch) */

    @Test
    void testExtractErrorMessageJsonBodyWithErrorsArrayJoinsMessages() throws Exception {
        String rawJson = "{\"status\":\"error\",\"errors\":[{\"message\":\"Bad thing\"}]}";
        CloseableHttpResponse response = httpResponse(403);
        when(response.getEntity()).thenReturn(new StringEntity(rawJson, ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            BrightspaceException exception = assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));

            assertEquals("Bad thing", exception.getBrightspaceErrorMessage());
        }
    }

    @Test
    void testExtractErrorMessageMalformedJsonFallsBackToRawBody() throws Exception {
        String rawBody = "not valid json";
        CloseableHttpResponse response = httpResponse(403);
        when(response.getEntity()).thenReturn(new StringEntity(rawBody, ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            BrightspaceException exception = assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));

            assertEquals(rawBody, exception.getBrightspaceErrorMessage());
        }
    }

    @Test
    void testExtractErrorMessageNonJsonContentTypeReturnsNullMessage() throws Exception {
        CloseableHttpResponse response = httpResponse(403);
        when(response.getEntity()).thenReturn(new StringEntity("forbidden", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            BrightspaceException exception = assertThrows(BrightspaceException.class, () -> simpleRestClient.sendApiGet(token(), URL, 100, 100));

            assertNull(exception.getBrightspaceErrorMessage());
        }
    }

    /* sendJsonPost / sendJsonPut */

    @Test
    void testSendJsonPostNonBlankJsonAttachesEntity() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("{\"ok\":true}", ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendJsonPost(token(), URL, "{\"a\":1}", 100, 100);

            assertEquals(200, result.getResponseCode());
            assertEquals("{\"ok\":true}", result.getContent());
        }

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        assertNotNull(((HttpPost) captor.getValue()).getEntity());
    }

    @Test
    void testSendJsonPostBlankJsonSkipsEntity() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendJsonPost(token(), URL, null, 100, 100);

            assertEquals(200, result.getResponseCode());
        }

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        assertNull(((HttpPost) captor.getValue()).getEntity());
    }

    @Test
    void testSendJsonPutNonBlankJsonReturnsContent() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("{}", ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendJsonPut(token(), URL, "{\"a\":1}", 100, 100);

            assertEquals(200, result.getResponseCode());
            assertEquals("{}", result.getContent());
        }
    }

    /* sendApiPost */

    @Test
    void testSendApiPostWithParametersBuildsFormEntity() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiPost(token(), URL, sampleParams(), 100, 100);

            assertEquals(200, result.getResponseCode());
            assertEquals("ok", result.getContent());
        }
    }

    @Test
    void testSendApiPostWithNullParametersUsesEmptyParamList() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiPost(token(), URL, null, 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    /* sendApiPut */

    @Test
    void testSendApiPutWithParametersBuildsFormEntity() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiPut(token(), URL, sampleParams(), 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    @Test
    void testSendApiPutWithNullParametersUsesEmptyParamList() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiPut(token(), URL, null, 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    /* sendApiDelete */

    @Test
    void testSendApiDeleteWithParametersBuildsFormEntity() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiDelete(token(), URL, sampleParams(), 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    @Test
    void testSendApiDeleteWithNullParametersUsesEmptyParamList() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("ok", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiDelete(token(), URL, null, 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    /* sendApiPostFile */

    @Test
    void testSendApiPostFileWithFilePathUsesFileBody() throws Exception {
        File tempFile = File.createTempFile("simple-rest-client-test", ".txt");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "file contents");

        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("uploaded", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient)) {
            Response result = simpleRestClient.sendApiPostFile(token(), URL, sampleParams(), "file", tempFile.getAbsolutePath(), null, 100, 100);

            assertEquals(200, result.getResponseCode());
            assertEquals("uploaded", result.getContent());
        }
    }

    @Test
    void testSendApiPostFileWithInputStreamUsesInputStreamBody() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        when(response.getEntity()).thenReturn(new StringEntity("uploaded", ContentType.TEXT_PLAIN));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (
            MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient);
            InputStream is = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))
        ) {
            Response result = simpleRestClient.sendApiPostFile(token(), URL, Map.of(), "file", "irrelevant.txt", is, 100, 100);

            assertEquals(200, result.getResponseCode());
        }
    }

    /* sendUpload */

    @Test
    void testSendUploadHappyPathReturnsLocationHeader() throws Exception {
        CloseableHttpResponse response = httpResponse(201);
        when(response.getFirstHeader("Location")).thenReturn(new BasicHeader("Location", "https://brightspace.example.edu/uploaded/1"));
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (
            MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient);
            InputStream is = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))
        ) {
            String location = simpleRestClient.sendUpload(URL, sampleParams(), is, "file.txt", 100, 100);

            assertEquals("https://brightspace.example.edu/uploaded/1", location);
        }
    }

    @Test
    void testSendUpload201WithoutLocationHeaderThrowsBrightspaceException() throws Exception {
        CloseableHttpResponse response = httpResponse(201);
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (
            MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient);
            InputStream is = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))
        ) {
            BrightspaceException exception = assertThrows(
                BrightspaceException.class,
                () -> simpleRestClient.sendUpload(URL, Map.of(), is, "file.txt", 100, 100)
            );

            assertTrue(exception.getMessage().contains("No location to redirect"));
        }
    }

    @Test
    void testSendUploadBadStatusThrowsBrightspaceException() throws Exception {
        CloseableHttpResponse response = httpResponse(200);
        CloseableHttpClient httpClient = mockClientReturning(response);

        try (
            MockedStatic<HttpClientBuilder> _ = mockHttpClientBuilder(httpClient);
            InputStream is = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))
        ) {
            BrightspaceException exception = assertThrows(
                BrightspaceException.class,
                () -> simpleRestClient.sendUpload(URL, Map.of(), is, "file.txt", 100, 100)
            );

            assertTrue(exception.getMessage().contains("Bad status"));
        }
    }

}
