package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.User;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValuePaged;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public class UserGradeValueServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "12345";
    private static final String GRADE_OBJECT_ID = "55";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private BrightspaceMessengerService brightspaceMessenger;
    @Mock private ResponseParserService responseParser;

    private ApiVersion apiVersion;
    private UserGradeValueServiceImpl userGradeValueService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        apiVersion = ApiVersion.builder().le("1.43").build();

        userGradeValueService = new UserGradeValueServiceImpl(BASE_URL, apiVersion, oauthToken, restClient, 5000, 5000, 50, false);

        userGradeValueService.brightspaceMessenger = brightspaceMessenger;
        userGradeValueService.responseParser = responseParser;
    }

    private UserGradeValue userGradeValue(String identifier) {
        return UserGradeValue.builder()
            .user(User.builder().identifier(identifier).build())
            .build();
    }

    private Response rawResponse(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    @Test
    public void testGetAllReturnsParsedGradeValues() throws IOException {
        Response page1 = rawResponse("raw-page-1");
        UserGradeValuePaged paged = UserGradeValuePaged.builder()
            .objects(List.of(userGradeValue("user-1"), userGradeValue("user-2")))
            .build();
        List<UserGradeValue> finalParsed = List.of(userGradeValue("user-1"), userGradeValue("user-2"));

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of(page1));
        when(responseParser.parseToObject(UserGradeValuePaged.class, page1)).thenReturn(Optional.of(paged));
        when(responseParser.<UserGradeValue>parseToList(any(), eq(page1))).thenReturn(finalParsed);

        List<UserGradeValue> result = userGradeValueService.getAll(ORG_UNIT_ID, GRADE_OBJECT_ID);

        assertEquals(2, result.size());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).get(eq(oauthToken), urlCaptor.capture(), isNull());
        assertEquals(
            BASE_URL + "/d2l/api/le/1.43/" + ORG_UNIT_ID + "/grades/" + GRADE_OBJECT_ID + "/values/",
            urlCaptor.getValue()
        );

        // the raw page's content must have been rewritten in-place to the serialized "Objects" array
        // before being handed to responseParser.parseToList
        assertTrue(page1.getContent().contains("user-1"));
        assertTrue(page1.getContent().contains("user-2"));
    }

    @Test
    public void testGetAllReturnsEmptyListWhenNoResponses() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of());

        List<UserGradeValue> result = userGradeValueService.getAll(ORG_UNIT_ID, GRADE_OBJECT_ID);

        assertTrue(result.isEmpty());
        verify(responseParser, never()).parseToObject(eq(UserGradeValuePaged.class), any());
    }

    @Test
    public void testGetAllRewritesContentToEmptyArrayWhenPagedObjectMissing() throws IOException {
        Response page1 = rawResponse("raw-page-1");

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of(page1));
        when(responseParser.parseToObject(UserGradeValuePaged.class, page1)).thenReturn(Optional.empty());
        when(responseParser.<UserGradeValue>parseToList(any(), eq(page1))).thenReturn(List.of());

        List<UserGradeValue> result = userGradeValueService.getAll(ORG_UNIT_ID, GRADE_OBJECT_ID);

        assertTrue(result.isEmpty());
        assertEquals("[]", page1.getContent());
    }

    @Test
    public void testGetAllPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> userGradeValueService.getAll(ORG_UNIT_ID, GRADE_OBJECT_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllWithCallbackInvokesRegisteredConsumer() throws IOException {
        // NOTE: per the real getListResponse() implementation, the per-page callback consumer is
        // handed to brightspaceMessenger.get() and (per BrightspaceMessengerServiceImpl) invoked
        // synchronously on each RAW page response, BEFORE the "unwrap Objects into content"
        // rewrite step runs. So the callback's call to responseParser.parseToList(listType(), response)
        // actually receives the raw paginated envelope (e.g. {"Next":...,"Objects":[...]}), not the
        // rewritten plain-array content the non-callback path parses. This looks like a real
        // inconsistency in UserGradeValueServiceImpl - documented here, not fixed.
        Response page1 = rawResponse("raw-page-1");
        List<UserGradeValue> callbackParsed = List.of(userGradeValue("user-9"));

        when(responseParser.<UserGradeValue>parseToList(any(), eq(page1))).thenReturn(callbackParsed);

        // simulate BrightspaceMessengerServiceImpl invoking the per-page callback synchronously
        // during the paginated get() call, before "responseCallback" is reset to null
        when(brightspaceMessenger.get(eq(oauthToken), any(), any())).thenAnswer(
            invocation -> {
                Consumer<Response> consumer = invocation.getArgument(2);

                if (consumer != null) {
                    consumer.accept(page1);
                }

                return List.of(page1);
            }
        );
        when(responseParser.parseToObject(UserGradeValuePaged.class, page1)).thenReturn(Optional.empty());

        Consumer<List<UserGradeValue>> externalCallback = mock(Consumer.class);
        userGradeValueService.withCallback(externalCallback);

        userGradeValueService.getAll(ORG_UNIT_ID, GRADE_OBJECT_ID);

        verify(externalCallback).accept(callbackParsed);
    }

    @Test
    public void testListTypeAndObjectType() {
        assertEquals(UserGradeValue.class, userGradeValueService.objectType());
        assertTrue(userGradeValueService.listType() != null);
    }

}
