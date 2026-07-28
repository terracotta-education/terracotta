package edu.iu.terracotta.connectors.canvas.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl.CanvasAdvantageAgsServiceImpl;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class CanvasAdvantageAgsServiceImplTest extends BaseTest {

    @InjectMocks private CanvasAdvantageAgsServiceImpl canvasAdvantageAgsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testGetToken() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageAgsService.getToken(LtiAgsScope.SCORE, platformDeployment)
        );
    }

    @Test
    public void testPutLineItem() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageAgsService.putLineItem(ltiToken, ltiContextEntity, lineItem)
        );
    }

    @Test
    public void testGetLineItem() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId")
        );
    }

    @Test
    public void testPostLineItems() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems)
        );
    }

    @Test
    public void testGetResults() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageAgsService.getResults(ltiToken, ltiContextEntity, "lineItemId")
        );
    }

    @Test
    public void testGetLineItemsSinglePage() throws ConnectionException {
        LineItems ret = canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertEquals(1, ret.getLineItemList().size());
        assertEquals(lineItem, ret.getLineItemList().get(0));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class));
    }

    @Test
    public void testGetLineItemsPaginatesUntilNoNextPage() throws ConnectionException {
        ResponseEntity<LineItem[]> nextPageResponse = new ResponseEntity<>(new LineItem[] {lineItem, lineItem}, HttpStatusCode.valueOf(200));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class)))
            .thenReturn(lineItemArrayResponseEntity, nextPageResponse);
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn("https://canvas.example.edu/next", (String) null);

        LineItems ret = canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertEquals(3, ret.getLineItemList().size());
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class));
    }

    @Test
    public void testGetLineItemsThrowsConnectionExceptionOnBadStatus() {
        when(lineItemArrayResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

    @Test
    public void testGetLineItemsThrowsConnectionExceptionOnNullBody() {
        when(lineItemArrayResponseEntity.getBody()).thenReturn(null);

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

    @Test
    public void testGetLineItemsWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

    @Test
    public void testDeleteLineItem() throws ConnectionException, TerracottaConnectorException {
        boolean ret = canvasAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId");

        assertTrue(ret);
    }

    @Test
    public void testDeleteLineItemThrowsConnectionExceptionOnBadStatus() {
        when(stringResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(404));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testDeleteLineItemWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(), eq(String.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testPostLineItemSuccess() throws ConnectionException {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenReturn(lineItemResponseEntity);
        when(lineItemResponseEntity.getBody()).thenReturn(lineItem);

        LineItem ret = canvasAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem);

        assertEquals(lineItem, ret);
    }

    @Test
    public void testPostLineItemThrowsConnectionExceptionOnBadStatus() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenReturn(lineItemResponseEntity);
        when(lineItemResponseEntity.getStatusCode()).thenReturn(HttpStatusCode.valueOf(500));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testPostLineItemWrapsUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(LineItem.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testPostScoreSuccess() {
        // postScore() serializes the score with a real (non-mocked) JsonMapper call; serializing a
        // raw Mockito mock can throw (byte-buddy mocks expose extra non-bean properties Jackson
        // chokes on), which would spuriously push this "success" test into the catch block instead
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class)))
            .thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        assertDoesNotThrow(() -> canvasAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", realScore));
        verify(restTemplate).exchange(eq("lineItemId/scores"), eq(HttpMethod.POST), any(), eq(Void.class));
    }

    @Test
    public void testPostScoreThrowsConnectionExceptionOnBadStatus() {
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class)))
            .thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(
            ConnectionException.class,
            () -> canvasAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", realScore)
        );
    }

    @Test
    public void testPostScoreWrapsUnexpectedException() {
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Void.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(
            ConnectionException.class,
            () -> canvasAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, "lineItemId", realScore)
        );
    }

    @Test
    public void testGetLineItemsPaginationValidatesNextPageStatus() throws ConnectionException {
        ResponseEntity<LineItem[]> erroredNextPageResponse = new ResponseEntity<>(new LineItem[] {lineItem}, HttpStatusCode.valueOf(500));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(LineItem[].class)))
            .thenReturn(lineItemArrayResponseEntity, erroredNextPageResponse);
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn("https://canvas.example.edu/next", (String) null);

        assertThrows(ConnectionException.class, () -> canvasAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

}
