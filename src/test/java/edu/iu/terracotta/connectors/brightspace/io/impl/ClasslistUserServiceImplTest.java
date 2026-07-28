package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUser;
import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUserPaged;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

/**
 * The constructor wires up real BrightspaceMessengerServiceImpl/ResponseParserServiceImpl
 * instances; both are replaced with mocks (the fields are protected and this test lives in the
 * same package) so behavior of ClasslistUserServiceImpl itself - not its collaborators - is what's
 * under test.
 *
 * Note that ClasslistUserServiceImpl#getListResponse re-serializes the "Objects" portion of a
 * parsed ClasslistUserPaged page back into the Response's content via a *real*, non-mocked
 * JsonMapper before handing it to responseParser.parseToList(); that step is exercised for real
 * here (see testGetAllHandlesEmptyParsedPageGracefully).
 */
public class ClasslistUserServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "6";

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private ClasslistUserServiceImpl classlistUserService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().le("1.30").lp("1.30").build();

        classlistUserService = new ClasslistUserServiceImpl(
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
        classlistUserService.brightspaceMessenger = brightspaceMessenger;
        classlistUserService.responseParser = responseParser;
    }

    private Response rawPage() {
        return Response.builder().responseCode(200).content("raw").build();
    }

    private ClasslistUser classlistUser(String identifier, String email) {
        return ClasslistUser.builder().identifier(identifier).email(email).displayName("User " + identifier).build();
    }

    @Test
    public void testGetAllSuccessWithRoleId() throws IOException {
        Response page = rawPage();
        ClasslistUser user1 = classlistUser("1", "one@example.com");
        ClasslistUser user2 = classlistUser("2", "two@example.com");

        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenReturn(List.of(page));
        when(responseParser.parseToObject(ClasslistUserPaged.class, page)).thenReturn(Optional.of(ClasslistUserPaged.builder().objects(List.of(user1, user2)).build()));
        when(responseParser.<ClasslistUser>parseToList(any(), eq(page))).thenReturn(List.of(user1, user2));

        List<UserExtended> result = classlistUserService.getAll(ORG_UNIT_ID, true, 5L);

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("one@example.com", result.get(0).getEmail());
        assertEquals("2", result.get(1).getId());
        assertEquals("two@example.com", result.get(1).getEmail());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).get(eq(oauthToken), urlCaptor.capture(), isNull());
        String url = urlCaptor.getValue();
        assertTrue(url.startsWith(BASE_URL + "/d2l/api/le/1.30/" + ORG_UNIT_ID + "/classlist/paged/"));
        assertTrue(url.contains("onlyShowShownInGrades=true"));
        assertTrue(url.contains("roleId=5"));
    }

    @Test
    public void testGetAllOmitsRoleIdParamWhenNull() throws IOException {
        Response page = rawPage();
        ClasslistUser user1 = classlistUser("1", "one@example.com");

        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenReturn(List.of(page));
        when(responseParser.parseToObject(ClasslistUserPaged.class, page)).thenReturn(Optional.of(ClasslistUserPaged.builder().objects(List.of(user1)).build()));
        when(responseParser.<ClasslistUser>parseToList(any(), eq(page))).thenReturn(List.of(user1));

        List<UserExtended> result = classlistUserService.getAll(ORG_UNIT_ID, false, null);

        assertEquals(1, result.size());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brightspaceMessenger).get(eq(oauthToken), urlCaptor.capture(), isNull());
        String url = urlCaptor.getValue();
        assertTrue(url.contains("onlyShowShownInGrades=false"));
        assertTrue(!url.contains("roleId="));
    }

    @Test
    public void testGetAllMergesMultiplePagedResponses() throws IOException {
        Response page1 = rawPage();
        Response page2 = rawPage();
        ClasslistUser user1 = classlistUser("1", "one@example.com");
        ClasslistUser user2 = classlistUser("2", "two@example.com");
        ClasslistUser user3 = classlistUser("3", "three@example.com");

        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenReturn(List.of(page1, page2));
        when(responseParser.parseToObject(ClasslistUserPaged.class, page1)).thenReturn(Optional.of(ClasslistUserPaged.builder().objects(List.of(user1)).build()));
        when(responseParser.parseToObject(ClasslistUserPaged.class, page2)).thenReturn(Optional.of(ClasslistUserPaged.builder().objects(List.of(user2, user3)).build()));
        when(responseParser.<ClasslistUser>parseToList(any(), eq(page1))).thenReturn(List.of(user1));
        when(responseParser.<ClasslistUser>parseToList(any(), eq(page2))).thenReturn(List.of(user2, user3));

        List<UserExtended> result = classlistUserService.getAll(ORG_UNIT_ID, true, null);

        assertEquals(3, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("2", result.get(1).getId());
        assertEquals("3", result.get(2).getId());
    }

    @Test
    public void testGetAllReturnsEmptyListWhenNoResponses() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenReturn(List.of());

        List<UserExtended> result = classlistUserService.getAll(ORG_UNIT_ID, true, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(responseParser, never()).parseToObject(any(), any());
        verify(responseParser, never()).parseToList(any(), any());
    }

    @Test
    public void testGetAllHandlesEmptyParsedPageGracefully() throws IOException {
        // responseParser.parseToObject() returning empty exercises the
        // classlistUserPaged.map(...).orElse(List.of()) fallback branch; the real (non-mocked)
        // JsonMapper inside getListResponse() then serializes that empty list into the response's
        // content, which we verify below rather than just trusting the (mocked) downstream parse.
        Response page = rawPage();

        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenReturn(List.of(page));
        when(responseParser.parseToObject(ClasslistUserPaged.class, page)).thenReturn(Optional.empty());
        when(responseParser.<ClasslistUser>parseToList(any(), eq(page))).thenReturn(List.of());

        List<UserExtended> result = classlistUserService.getAll(ORG_UNIT_ID, true, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals("[]", page.getContent());
    }

    @Test
    public void testGetAllPropagatesIOExceptionFromMessenger() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> classlistUserService.getAll(ORG_UNIT_ID, true, null));
    }

    @Test
    public void testGetAllPropagatesInvalidOauthTokenException() throws IOException {
        when(brightspaceMessenger.get(eq(oauthToken), anyString(), isNull())).thenThrow(new InvalidOauthTokenException());

        assertThrows(InvalidOauthTokenException.class, () -> classlistUserService.getAll(ORG_UNIT_ID, true, null));
    }

    @Test
    public void testListTypeAndObjectType() {
        assertNotNull(classlistUserService.listType());
        assertEquals(ClasslistUser.class, classlistUserService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsField() {
        ClasslistUserReaderService result = classlistUserService.withCallback(list -> { });

        assertSame(classlistUserService, result);
        assertNotNull(classlistUserService.responseCallback);
    }

}
