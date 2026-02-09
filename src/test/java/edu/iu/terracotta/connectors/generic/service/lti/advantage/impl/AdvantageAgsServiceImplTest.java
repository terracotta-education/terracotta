package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Result;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Results;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class AdvantageAgsServiceImplTest extends BaseTest {

    @InjectMocks private AdvantageAgsServiceImpl advantageAgsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    public void testGetTokenLineItem() throws ConnectionException {
        advantageAgsService.getToken(LtiAgsScope.LINEITEM, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_LINEITEM.key());
    }

    @Test
    public void testGetTokenResults() throws ConnectionException {
        advantageAgsService.getToken(LtiAgsScope.RESULTS, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_RESULT_READONLY.key());
    }

    @Test
    public void testGetTokenScores() throws ConnectionException {
        advantageAgsService.getToken(LtiAgsScope.SCORES, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_SCORE.key());
    }

    @Test
    public void testGetLineItems() throws ConnectionException, TerracottaConnectorException {
        LineItems lineItems = advantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertNotNull(lineItems);
        assertEquals(1, lineItems.getLineItemList().size());
    }

    @Test
    public void testGetLineItemsNextPage() throws ConnectionException, TerracottaConnectorException {
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        LineItems lineItems = advantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertNotNull(lineItems);
        assertEquals(1, lineItems.getLineItemList().size());
    }

    @Test
    public void testPutLineItem() throws ConnectionException {
        LineItem ret = advantageAgsService.putLineItem(ltiToken, ltiContextEntity, lineItem);

        assertEquals(lineItem, ret);
    }

    @Test
    public void testPutLineItemBadRequest() throws ConnectionException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(LineItem.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> advantageAgsService.putLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testGetLineItem() throws ConnectionException {
        LineItem ret = advantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId");

        assertEquals(lineItem, ret);
    }

    @Test
    public void testGetLineItemBadRequest() throws ConnectionException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> advantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testPostLineItems() throws ConnectionException {
        LineItems ret = advantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems);

        assertNotNull(ret);
        assertEquals(1, ret.getLineItemList().size());
    }

    @Test
    public void testPostLineItemsNextPage() throws ConnectionException {
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        LineItems ret = advantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems);

        assertNotNull(ret);
        assertEquals(2, ret.getLineItemList().size());
    }

    @Test
    public void testPostLineItemsBadRequest() throws ConnectionException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(LineItem[].class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> advantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems));
    }

    @Test
    public void testGetResults() throws ConnectionException {
        Results rets = advantageAgsService.getResults(ltiToken, ltiContextEntity, "lineItemId");

        assertNotNull(rets);
        assertEquals(1, rets.getResultList().size());
    }

    @Test
    public void testGetResultsNextPage() throws ConnectionException {
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        Results rets = advantageAgsService.getResults(ltiToken, ltiContextEntity, "lineItemId");

        assertNotNull(rets);
        assertEquals(2, rets.getResultList().size());
    }

    @Test
    public void testGetResultsBadRequest() throws ConnectionException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Result[].class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> advantageAgsService.getResults(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testPostScoreUsingCanvas() throws ConnectionException, TerracottaConnectorException {
        when(advantageAgsConnectorService.instance(any(PlatformDeployment.class), eq(AdvantageAgsService.class))).thenReturn(canvasAdvantageAgsService);

        advantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", score);

        verify(canvasAdvantageAgsService).postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", score);
    }

}