package edu.iu.terracotta.connectors.brightspace.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.brightspace.service.lti.advantage.impl.BrightspaceAdvantageAgsServiceImpl;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class BrightspaceAdvantageAgsServiceImplTest extends BaseTest {

    @InjectMocks private BrightspaceAdvantageAgsServiceImpl brightspaceAdvantageAgsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testGetTokenLineItemScope() throws ConnectionException {
        var ret = brightspaceAdvantageAgsService.getToken(LtiAgsScope.LINEITEM, platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem");
    }

    @Test
    public void testGetTokenResultsScope() throws ConnectionException {
        var ret = brightspaceAdvantageAgsService.getToken(LtiAgsScope.RESULTS, platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly");
    }

    @Test
    public void testGetTokenScoresScope() throws ConnectionException {
        var ret = brightspaceAdvantageAgsService.getToken(LtiAgsScope.SCORES, platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-ags/scope/score");
    }

    @Test
    public void testGetTokenDefaultsToLineItemScopeForUnmatchedType() throws ConnectionException {
        // SCORE (singular) is not one of the switch's LINEITEM/RESULTS/SCORES cases, so it falls through to default
        var ret = brightspaceAdvantageAgsService.getToken(LtiAgsScope.SCORE, platformDeployment);

        assertEquals(ltiToken, ret);
        verify(advantageConnectorHelper).getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem");
    }

    @Test
    public void testGetLineItemsHappyPathSinglePage() throws ConnectionException {
        LineItems ret = brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertEquals(1, ret.getLineItemList().size());
        assertEquals(lineItem, ret.getLineItemList().get(0));
    }

    @Test
    public void testGetLineItemsSetsResourceLinkIdWhenBlank() throws ConnectionException {
        when(lineItem.getResourceLinkId()).thenReturn("");
        when(lineItem.getId()).thenReturn("lineitem-id-1");

        brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        verify(lineItem).setResourceLinkId("lineitem-id-1");
    }

    @Test
    public void testGetLineItemsDoesNotOverwriteResourceLinkIdWhenPresent() throws ConnectionException {
        brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        verify(lineItem, never()).setResourceLinkId(anyString());
    }

    @Test
    public void testGetLineItemsPaginatesUntilNoNextPage() throws ConnectionException {
        when(advantageConnectorHelper.nextPage(any())).thenReturn("https://brightspace.example.edu/next", (String) null);

        LineItems ret = brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertEquals(2, ret.getLineItemList().size());
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class));
    }

    @Test
    public void testGetLineItemsThrowsConnectionExceptionOnBadStatus() {
        when(lineItemArrayResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-bad-status");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity)
        );

        assertEquals("wrapped-bad-status", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the AGS"), any(ConnectionException.class));
    }

    @Test
    public void testGetLineItemsWrapsExceptionWhenBodyIsNull() {
        when(lineItemArrayResponseEntity.getBody()).thenReturn(null);
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-null-body");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity)
        );

        assertEquals("wrapped-null-body", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the AGS"), any(NullPointerException.class));
    }

    @Test
    public void testGetLineItemsWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class))).thenThrow(new RuntimeException("boom"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-boom");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity)
        );

        assertEquals("wrapped-boom", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the AGS"), any(RuntimeException.class));
    }

    @Test
    public void testDeleteLineItem() throws ConnectionException, TerracottaConnectorException {
        boolean ret = brightspaceAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId");

        assertTrue(ret);
    }

    @Test
    public void testDeleteLineItemThrowsConnectionExceptionOnBadStatus() {
        when(stringResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-delete-bad-status");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId")
        );

        assertEquals("wrapped-delete-bad-status", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't delete the lineitem with ID: [lineItemId]"), any(ConnectionException.class));
    }

    @Test
    public void testDeleteLineItemWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(), eq(String.class))).thenThrow(new RuntimeException("boom"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-delete-boom");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId")
        );

        assertEquals("wrapped-delete-boom", exception.getMessage());
    }

    @Test
    public void testPutLineItemIsUnsupported() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> brightspaceAdvantageAgsService.putLineItem(ltiToken, ltiContextEntity, lineItem)
        );
    }

    @Test
    public void testGetLineItemHappyPath() throws ConnectionException {
        LineItem ret = brightspaceAdvantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId");

        assertEquals(lineItem, ret);
    }

    @Test
    public void testGetLineItemThrowsConnectionExceptionOnBadStatus() {
        when(lineItemResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(404));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-get-lineitem-bad-status");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId")
        );

        assertEquals("wrapped-get-lineitem-bad-status", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the lineitem: [lineItemId]"), any(ConnectionException.class));
    }

    @Test
    public void testGetLineItemWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem.class))).thenThrow(new RuntimeException("boom"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-get-lineitem-boom");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId")
        );

        assertEquals("wrapped-get-lineitem-boom", exception.getMessage());
    }

    @Test
    public void testPostLineItemHappyPath() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(any(), any(LineItem.class), anyString(), anyString())).thenReturn(lineItemHttpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenReturn(lineItemResponseEntity);

        LineItem ret = brightspaceAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem);

        assertEquals(lineItem, ret);
    }

    @Test
    public void testPostLineItemThrowsConnectionExceptionOnBadStatus() {
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(any(), any(LineItem.class), anyString(), anyString())).thenReturn(lineItemHttpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));
        when(lineItem.getId()).thenReturn("lineitem-id-1");
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-post-lineitem-bad-status");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem)
        );

        assertEquals("wrapped-post-lineitem-bad-status", exception.getMessage());
    }

    @Test
    public void testPostLineItemWrapsUnexpectedException() {
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(any(), any(LineItem.class), anyString(), anyString())).thenReturn(lineItemHttpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenThrow(new RuntimeException("boom"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-post-lineitem-boom");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem)
        );

        assertEquals("wrapped-post-lineitem-boom", exception.getMessage());
    }

    @Test
    public void testPostLineItemsIsUnsupported() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> brightspaceAdvantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems)
        );
    }

    @Test
    public void testGetResultsIsUnsupported() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> brightspaceAdvantageAgsService.getResults(ltiToken, ltiContextEntity, "lineItemId")
        );
    }

    @Test
    public void testPostScoreHappyPath() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(any(), anyString())).thenReturn(new HttpEntity<>("payload"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        brightspaceAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", score);

        verify(restTemplate).exchange(eq("lineItemId/scores"), eq(HttpMethod.POST), any(), eq(Void.class));
    }

    @Test
    public void testPostScoreThrowsConnectionExceptionOnBadStatus() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(any(), anyString())).thenReturn(new HttpEntity<>("payload"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-post-score-bad-status");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", score)
        );

        assertEquals("wrapped-post-score-bad-status", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't post scores"), any(ConnectionException.class));
    }

    @Test
    public void testPostScoreWrapsUnexpectedException() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(any(), anyString())).thenReturn(new HttpEntity<>("payload"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class))).thenThrow(new RuntimeException("boom"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any())).thenReturn("wrapped-post-score-boom");

        ConnectionException exception = assertThrows(
            ConnectionException.class,
            () -> brightspaceAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", score)
        );

        assertEquals("wrapped-post-score-boom", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Can't post scores"), any(RuntimeException.class));
    }

}
