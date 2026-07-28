package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import edu.iu.terracotta.connectors.canvas.dao.model.extended.ConversationExtended;
import edu.ksu.canvas.model.Conversation;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.CreateConversationOptions;
import edu.ksu.canvas.requestOptions.GetSingleConversationOptions;

public class ConversationExtendedImplTest {

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;

    private ConversationExtendedImpl conversationExtended;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        conversationExtended = new ConversationExtendedImpl("https://canvas.example.com", 1, oauthToken, restClient, 1000, 1000, 100, false);
    }

    private Response buildResponse(boolean errorHappened, int responseCode, String content, String nextLink) {
        Response response = new Response();
        response.setErrorHappened(errorHappened);
        response.setResponseCode(responseCode);
        response.setContent(content);
        response.setNextLink(nextLink);

        return response;
    }

    @Test
    public void testCreateConversationReturnsParsedConversations() throws Exception {
        Response response = buildResponse(
            false,
            200,
            "[{\"id\":1,\"subject\":\"Hi\"},{\"id\":2,\"subject\":\"Hello\"}]",
            null
        );

        when(restClient.sendApiPost(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        List<ConversationExtended> conversations = conversationExtended.createConversation(new CreateConversationOptions("2", "Hello there"));

        assertEquals(2, conversations.size());
        assertEquals("1", conversations.get(0).getId());
        assertEquals("2", conversations.get(1).getId());
        // NOTE: unlike getSingleConversation, createConversation's parseList(..) never sets .type(Conversation.class)
        // on the returned ConversationExtended items. Documenting this existing inconsistency rather than fixing it.
        assertNull(conversations.get(0).getType());
    }

    @Test
    public void testCreateConversationReturnsEmptyListWhenNoConversationsCreated() throws Exception {
        Response response = buildResponse(false, 200, "[]", null);

        when(restClient.sendApiPost(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        List<ConversationExtended> conversations = conversationExtended.createConversation(new CreateConversationOptions("2", "Hello there"));

        assertTrue(conversations.isEmpty());
    }

    @Test
    public void testCreateConversationThrowsWhenCanvasReturnsErrorWithNoBody() throws Exception {
        // NOTE: neither ConversationImpl.createConversation nor ConversationExtendedImpl.parseList check
        // for a Canvas error/empty body before use. A null response body causes GsonResponseParser.parseToList
        // to return a null List, and parseList(..) then calls .forEach(..) on that null, throwing NPE.
        // Documenting this existing behavior rather than fixing it.
        Response response = buildResponse(true, 500, null, null);

        when(restClient.sendApiPost(eq(oauthToken), anyString(), any(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(NullPointerException.class, () -> conversationExtended.createConversation(new CreateConversationOptions("2", "Hello there")));
    }

    @Test
    public void testGetSingleConversationReturnsParsedConversation() throws Exception {
        Response response = buildResponse(false, 200, "{\"id\":5,\"subject\":\"Test Subject\"}", null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        Optional<ConversationExtended> result = conversationExtended.getSingleConversation(new GetSingleConversationOptions(5L));

        assertTrue(result.isPresent());
        assertEquals("5", result.get().getId());
        assertEquals(Conversation.class, result.get().getType());
    }

    @Test
    public void testGetSingleConversationThrowsWhenCanvasReturnsErrorWithNoBody() throws Exception {
        // NOTE: ConversationImpl.getSingleConversation does not check response.getErrorHappened()/getResponseCode()
        // before parsing. A null body makes GsonResponseParser.parseToObject wrap a null in Optional.of(null),
        // which throws NPE immediately - before parseOptional(..) is even reached. Documenting existing behavior.
        Response response = buildResponse(true, 404, null, null);

        when(restClient.sendApiGet(eq(oauthToken), anyString(), anyInt(), anyInt())).thenReturn(response);

        assertThrows(NullPointerException.class, () -> conversationExtended.getSingleConversation(new GetSingleConversationOptions(999L)));
    }

}
