package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageQuickLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageQuickLink;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class LtiAdvantageQuickLinkServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";
    private static final String ORG_UNIT_ID = "12345";
    private static final long LTI_ADVANTAGE_LINK_ID = 555L;

    private BrightspaceMessengerService brightspaceMessenger;
    private ResponseParserService responseParser;
    private OauthToken oauthToken;
    private ApiVersion apiVersion;

    private LtiAdvantageQuickLinkServiceImpl ltiAdvantageQuickLinkService;

    @BeforeEach
    public void beforeEach() {
        oauthToken = mock(OauthToken.class);
        apiVersion = ApiVersion.builder().lp("1.30").le("1.43").build();

        ltiAdvantageQuickLinkService = new LtiAdvantageQuickLinkServiceImpl(
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
        // of LtiAdvantageQuickLinkServiceImpl itself, not its collaborators, is what's under test.
        brightspaceMessenger = mock(BrightspaceMessengerService.class);
        responseParser = mock(ResponseParserService.class);
        ltiAdvantageQuickLinkService.brightspaceMessenger = brightspaceMessenger;
        ltiAdvantageQuickLinkService.responseParser = responseParser;
    }

    private String expectedRootUrl() {
        return String.format(
            "%s/d2l/api/le/%s/ltiadvantage/quicklinks/orgunit/%s/link/%s",
            BASE_URL,
            apiVersion.getLe(),
            ORG_UNIT_ID,
            LTI_ADVANTAGE_LINK_ID
        );
    }

    @Test
    public void testCreateSuccess() throws IOException {
        LtiAdvantageQuickLink quickLink = LtiAdvantageQuickLink.builder().linkId(LTI_ADVANTAGE_LINK_ID).publicUrl("https://brightspace.example.com/public").build();
        Response response = Response.builder().responseCode(200).content("{}").build();

        when(brightspaceMessenger.sendJsonPost(eq(oauthToken), eq(expectedRootUrl()), isNull())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageQuickLink.class, response)).thenReturn(Optional.of(quickLink));

        Optional<LtiAdvantageQuickLink> result = ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, LTI_ADVANTAGE_LINK_ID);

        assertTrue(result.isPresent());
        assertEquals(quickLink, result.get());
        verify(brightspaceMessenger, times(1)).sendJsonPost(eq(oauthToken), eq(expectedRootUrl()), isNull());
    }

    @Test
    public void testCreateThrowsWhenParserReturnsEmpty() throws IOException {
        Response response = Response.builder().responseCode(200).content("").build();

        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenReturn(response);
        when(responseParser.parseToObject(LtiAdvantageQuickLink.class, response)).thenReturn(Optional.empty());

        IOException exception = assertThrows(IOException.class, () -> ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, LTI_ADVANTAGE_LINK_ID));
        assertEquals("Error creating LTI Advantage Quick Link", exception.getMessage());
    }

    @Test
    public void testCreatePropagatesMessengerIOException() throws IOException {
        when(brightspaceMessenger.sendJsonPost(any(), any(), any())).thenThrow(new IOException("network error"));

        assertThrows(IOException.class, () -> ltiAdvantageQuickLinkService.create(ORG_UNIT_ID, LTI_ADVANTAGE_LINK_ID));
    }

    @Test
    public void testListType() {
        assertNotNull(ltiAdvantageQuickLinkService.listType());
        assertEquals(
            new TypeReference<List<LtiAdvantageQuickLink>>() {}.getType().toString(),
            ltiAdvantageQuickLinkService.listType().getType().toString()
        );
    }

    @Test
    public void testObjectType() {
        assertEquals(LtiAdvantageQuickLink.class, ltiAdvantageQuickLinkService.objectType());
    }

    @Test
    public void testWithCallbackReturnsSameInstanceAndSetsCallback() {
        LtiAdvantageQuickLinkReaderService result = ltiAdvantageQuickLinkService.withCallback(list -> { });

        assertSame(ltiAdvantageQuickLinkService, result);
        assertNotNull(ltiAdvantageQuickLinkService.responseCallback);
    }

}
