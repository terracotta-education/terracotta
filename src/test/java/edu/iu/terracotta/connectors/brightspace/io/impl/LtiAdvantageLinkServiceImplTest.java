package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLinkUpdate;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class LtiAdvantageLinkServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "12345";
    private static final long LINK_ID = 999L;

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private LtiAdvantageLinkServiceImpl ltiAdvantageLinkService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().lp("1.30").le("1.43").build();

        ltiAdvantageLinkService = new LtiAdvantageLinkServiceImpl(
            BASE_URL,
            apiVersion,
            oauthToken,
            mock(RestClient.class),
            30,
            30,
            10,
            false
        );

        // the constructor wires up real BrightspaceMessengerServiceImpl/ResponseParserServiceImpl instances;
        // replace with mocks (fields are protected and this test lives in the same package) so behavior
        // of LtiAdvantageLinkServiceImpl itself, not its collaborators, is what's under test.
        brightspaceMessenger = mock(BrightspaceMessengerService.class);
        responseParser = mock(ResponseParserService.class);
        ltiAdvantageLinkService.brightspaceMessenger = brightspaceMessenger;
        ltiAdvantageLinkService.responseParser = responseParser;
    }

    private String expectedRootUrl() {
        return String.format("%s/d2l/api/le/%s/ltiadvantage/links/orgunit/%s/", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID);
    }

    private String expectedItemUrl() {
        return String.format("%s/d2l/api/le/%s/ltiadvantage/links/orgunit/%s/%s", BASE_URL, apiVersion.getLe(), ORG_UNIT_ID, LINK_ID);
    }

    @Test
    public void testCreateSuccess() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("link name").build();
        LtiAdvantageLink link = LtiAdvantageLink.builder().linkId(LINK_ID).name("link name").build();
        Response response = Response.builder().responseCode(200).content("{}").build();

        when(brightspaceMessenger.sendJsonPost(eq(oauthToken), eq(expectedRootUrl()), eq(update.toJson(false)))).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.of(link));

        Optional<LtiAdvantageLink> result = ltiAdvantageLinkService.create(ORG_UNIT_ID, update);

        assertTrue(result.isPresent());
        assertEquals(link, result.get());
        verify(brightspaceMessenger, times(1)).sendJsonPost(eq(oauthToken), eq(expectedRootUrl()), anyString());
    }

    @Test
    public void testCreateThrowsWhenParserReturnsEmpty() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("link name").build();
        Response response = Response.builder().responseCode(200).content("").build();

        when(brightspaceMessenger.sendJsonPost(any(), anyString(), any())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> ltiAdvantageLinkService.create(ORG_UNIT_ID, update));
        assertEquals("Error creating LTI Advantage Link", exception.getMessage());
    }

    @Test
    public void testCreatePropagatesMessengerIOException() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("link name").build();

        when(brightspaceMessenger.sendJsonPost(any(), anyString(), any())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> ltiAdvantageLinkService.create(ORG_UNIT_ID, update));
    }

    @Test
    public void testGetSuccess() throws IOException {
        LtiAdvantageLink link = LtiAdvantageLink.builder().linkId(LINK_ID).name("existing").build();
        Response response = Response.builder().responseCode(200).content("{}").build();

        when(brightspaceMessenger.getSingleResponse(oauthToken, expectedItemUrl())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.of(link));

        Optional<LtiAdvantageLink> result = ltiAdvantageLinkService.get(ORG_UNIT_ID, LINK_ID);

        assertTrue(result.isPresent());
        assertEquals(link, result.get());
    }

    @Test
    public void testGetReturnsEmptyWhenNotFound() throws IOException {
        Response response = Response.builder().responseCode(200).content("").build();

        when(brightspaceMessenger.getSingleResponse(oauthToken, expectedItemUrl())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.empty());

        Optional<LtiAdvantageLink> result = ltiAdvantageLinkService.get(ORG_UNIT_ID, LINK_ID);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetPropagatesMessengerIOException() throws IOException {
        when(brightspaceMessenger.getSingleResponse(any(), anyString())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> ltiAdvantageLinkService.get(ORG_UNIT_ID, LINK_ID));
    }

    @Test
    public void testUpdateSuccess() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("updated name").build();
        LtiAdvantageLink link = LtiAdvantageLink.builder().linkId(LINK_ID).name("updated name").build();
        Response response = Response.builder().responseCode(200).content("{}").build();

        when(brightspaceMessenger.sendJsonPut(eq(oauthToken), eq(expectedItemUrl()), eq(update.toJson(false)))).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.of(link));

        Optional<LtiAdvantageLink> result = ltiAdvantageLinkService.update(ORG_UNIT_ID, LINK_ID, update);

        assertTrue(result.isPresent());
        assertEquals(link, result.get());
    }

    @Test
    public void testUpdateReturnsEmptyWhenParserReturnsEmpty() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("updated name").build();
        Response response = Response.builder().responseCode(200).content("").build();

        when(brightspaceMessenger.sendJsonPut(any(), anyString(), any())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageLink.class, response)).thenReturn(Optional.empty());

        Optional<LtiAdvantageLink> result = ltiAdvantageLinkService.update(ORG_UNIT_ID, LINK_ID, update);

        assertFalse(result.isPresent());
    }

    @Test
    public void testUpdatePropagatesMessengerIOException() throws IOException {
        LtiAdvantageLinkUpdate update = LtiAdvantageLinkUpdate.builder().name("updated name").build();

        when(brightspaceMessenger.sendJsonPut(any(), anyString(), any())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> ltiAdvantageLinkService.update(ORG_UNIT_ID, LINK_ID, update));
    }

    @Test
    public void testDeleteSuccess() throws IOException {
        ltiAdvantageLinkService.delete(ORG_UNIT_ID, LINK_ID);

        verify(brightspaceMessenger, times(1)).delete(oauthToken, expectedItemUrl());
    }

    @Test
    public void testDeletePropagatesMessengerIOException() throws IOException {
        when(brightspaceMessenger.delete(any(), anyString())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> ltiAdvantageLinkService.delete(ORG_UNIT_ID, LINK_ID));
    }

    @Test
    public void testListType() {
        assertNotNull(ltiAdvantageLinkService.listType());
        assertEquals(
            new TypeReference<List<LtiAdvantageLink>>() {}.getType().toString(),
            ltiAdvantageLinkService.listType().getType().toString()
        );
    }

    @Test
    public void testObjectType() {
        assertEquals(LtiAdvantageLink.class, ltiAdvantageLinkService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsCallback() {
        LtiAdvantageLinkReaderService result = ltiAdvantageLinkService.withCallback(list -> { });

        assertSame(ltiAdvantageLinkService, result);
        assertNotNull(ltiAdvantageLinkService.responseCallback);
    }

    @Test
    public void testDeleteDoesNotTouchResponseParser() throws IOException {
        ltiAdvantageLinkService.delete(ORG_UNIT_ID, LINK_ID);

        verify(responseParser, never()).parseToObject(any(), any());
    }

}
