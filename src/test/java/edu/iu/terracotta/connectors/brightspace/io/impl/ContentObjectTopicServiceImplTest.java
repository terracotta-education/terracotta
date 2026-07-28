package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopicUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

/**
 * The constructor wires up real BrightspaceMessengerServiceImpl/ResponseParserServiceImpl
 * instances; both are replaced with mocks (the fields are protected and this test lives in the
 * same package) so behavior of ContentObjectTopicServiceImpl itself - not its collaborators - is
 * what's under test.
 *
 * Note this class never calls the inherited get()/getList() helpers - create/get/update/delete all
 * talk to brightspaceMessenger directly - but listType()/objectType() are still implemented (as
 * required by the abstract base class) even though nothing in this class invokes them.
 */
public class ContentObjectTopicServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "6";
    private static final long CONTENT_OBJECT_MODULE_ID = 42L;
    private static final long CONTENT_OBJECT_TOPIC_ID = 9L;

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private ContentObjectTopicServiceImpl contentObjectTopicService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().le("1.30").lp("1.30").build();

        contentObjectTopicService = new ContentObjectTopicServiceImpl(
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
        contentObjectTopicService.brightspaceMessenger = brightspaceMessenger;
        contentObjectTopicService.responseParser = responseParser;
    }

    private String structureUrl() {
        return String.format("%s/d2l/api/le/%s/%s/content/modules/%s/structure/", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID);
    }

    private String topicUrl(long id) {
        return String.format("%s/d2l/api/le/%s/%s/content/topics/%s", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID, id);
    }

    private Response response(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    private ContentObjectTopic topic(long id, String title) {
        return ContentObjectTopic.builder().id(id).title(title).build();
    }

    private ContentObjectTopicUpdate anyUpdate() {
        return ContentObjectTopicUpdate.builder().title("New Topic").shortTitle("NT").build();
    }

    @Test
    public void testCreateSuccess() throws IOException {
        Response response = response("{}");
        ContentObjectTopic created = topic(1L, "New Topic");

        when(brightspaceMessenger.sendJsonPost(eq(oauthToken), eq(structureUrl()), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.of(created));

        Optional<ContentObjectTopic> result = contentObjectTopicService.create(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(created, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPost(eq(oauthToken), eq(structureUrl()), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Topic"));
    }

    @Test
    public void testCreateThrowsIOExceptionWhenResponseCannotBeParsed() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> contentObjectTopicService.create(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate()));
        assertEquals("Error creating Content Object Topic", exception.getMessage());
    }

    @Test
    public void testCreatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenThrow(new IOException("network down"));

        assertThrows(IOException.class, () -> contentObjectTopicService.create(ORG_UNIT_ID, CONTENT_OBJECT_MODULE_ID, anyUpdate()));
    }

    @Test
    public void testGetSuccess() throws IOException {
        Response response = response("single");
        ContentObjectTopic existing = topic(CONTENT_OBJECT_TOPIC_ID, "Existing Topic");

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), eq(topicUrl(CONTENT_OBJECT_TOPIC_ID)))).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.of(existing));

        Optional<ContentObjectTopic> result = contentObjectTopicService.get(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID);

        assertTrue(result.isPresent());
        assertSame(existing, result.get());
    }

    @Test
    public void testGetReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.getSingleResponse(any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.empty());

        Optional<ContentObjectTopic> result = contentObjectTopicService.get(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.getSingleResponse(any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> contentObjectTopicService.get(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID));
    }

    @Test
    public void testUpdateSuccess() throws IOException {
        Response response = response("updated");
        ContentObjectTopic updated = topic(CONTENT_OBJECT_TOPIC_ID, "Updated Topic");

        when(brightspaceMessenger.sendJsonPut(eq(oauthToken), eq(topicUrl(CONTENT_OBJECT_TOPIC_ID)), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.of(updated));

        Optional<ContentObjectTopic> result = contentObjectTopicService.update(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID, anyUpdate());

        assertTrue(result.isPresent());
        assertSame(updated, result.get());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).sendJsonPut(eq(oauthToken), eq(topicUrl(CONTENT_OBJECT_TOPIC_ID)), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("New Topic"));
    }

    @Test
    public void testUpdateReturnsEmptyOptionalWhenParserReturnsEmpty() throws IOException {
        Response response = response("");

        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(ContentObjectTopic.class, response)).thenReturn(Optional.empty());

        Optional<ContentObjectTopic> result = contentObjectTopicService.update(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID, anyUpdate());

        assertFalse(result.isPresent());
    }

    @Test
    public void testUpdatePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.sendJsonPut(any(), any(), any())).thenThrow(new IOException("timeout"));

        assertThrows(IOException.class, () -> contentObjectTopicService.update(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID, anyUpdate()));
    }

    @Test
    public void testDeleteSendsDeleteRequestToCorrectUrl() throws IOException {
        contentObjectTopicService.delete(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID);

        verify(brightspaceMessenger, times(1)).delete(eq(oauthToken), eq(topicUrl(CONTENT_OBJECT_TOPIC_ID)));
    }

    @Test
    public void testDeletePropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.delete(any(), any())).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> contentObjectTopicService.delete(ORG_UNIT_ID, CONTENT_OBJECT_TOPIC_ID));
    }

    @Test
    public void testListTypeAndObjectType() {
        assertNotNull(contentObjectTopicService.listType());
        assertEquals(ContentObjectTopic.class, contentObjectTopicService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsField() {
        ContentObjectTopicReaderService result = contentObjectTopicService.withCallback(list -> { });

        assertSame(contentObjectTopicService, result);
        assertNotNull(contentObjectTopicService.responseCallback);
    }

}
