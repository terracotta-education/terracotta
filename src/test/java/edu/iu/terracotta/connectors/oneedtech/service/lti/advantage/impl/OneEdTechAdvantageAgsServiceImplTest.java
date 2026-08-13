package edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class OneEdTechAdvantageAgsServiceImplTest extends BaseTest {

    @InjectMocks private OneEdTechAdvantageAgsServiceImpl oneEdTechAdvantageAgsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // These overloads are not stubbed anywhere in BaseServiceTest/BaseModelTest, and would
        // otherwise return null. Since anyString()/any(SomeClass.class) do NOT match null arguments,
        // an unstubbed null HttpEntity passed to restTemplate.exchange(...) would fail to match the
        // any(HttpEntity.class) matcher used in the default response stubs below, breaking the
        // "success" scenarios in a way unrelated to what's under test.
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(any(LtiToken.class), anyString(), anyString())).thenReturn(httpEntity);
        when(advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(any(LtiToken.class), any(LineItem.class), anyString(), anyString())).thenReturn(lineItemHttpEntity);
        when(advantageConnectorHelper.createTokenizedRequestEntity(any(LtiToken.class), anyString())).thenReturn(new HttpEntity<>("body"));
    }

    @Test
    public void testGetTokenLineItem() throws ConnectionException {
        oneEdTechAdvantageAgsService.getToken(LtiAgsScope.LINEITEM, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_LINEITEM.key());
    }

    @Test
    public void testGetTokenResults() throws ConnectionException {
        oneEdTechAdvantageAgsService.getToken(LtiAgsScope.RESULTS, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_RESULT_READONLY.key());
    }

    @Test
    public void testGetTokenScores() throws ConnectionException {
        oneEdTechAdvantageAgsService.getToken(LtiAgsScope.SCORES, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_SCORE.key());
    }

    @Test
    public void testGetTokenDefault() throws ConnectionException {
        oneEdTechAdvantageAgsService.getToken(LtiAgsScope.SINGLE, platformDeployment);

        verify(advantageConnectorHelper).getToken(platformDeployment, LtiAgsScope.AGS_LINEITEM.key());
    }

    @Test
    public void testPostLineItemSuccess() throws ConnectionException {
        LineItem responseLineItem = LineItem.builder()
            .id("li1")
            .label("label1")
            .scoreMaximum(100F)
            .build();
        ResponseEntity<LineItem> okResponse = new ResponseEntity<>(responseLineItem, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(LineItem.class))).thenReturn(okResponse);

        LineItem ret = oneEdTechAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem);

        assertEquals(responseLineItem, ret);
    }

    @Test
    public void testPostLineItemBadRequest() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(LineItem.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testPostLineItemException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(LineItem.class))).thenThrow(new RuntimeException("connection failure"));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.postLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testGetLineItems() throws ConnectionException, TerracottaConnectorException {
        LineItems ret = oneEdTechAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertNotNull(ret);
        assertEquals(1, ret.getLineItemList().size());
        // 1EdTech doesn't return resourceLinkId, so postLineItems sets it from the id field
        verify(lineItem).setResourceLinkId(lineItem.getId());
    }

    @Test
    public void testGetLineItemsNextPage() throws ConnectionException, TerracottaConnectorException {
        when(advantageConnectorHelper.nextPage(any(HttpHeaders.class))).thenReturn(LTI_URL, (String) null);

        LineItems ret = oneEdTechAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity);

        assertNotNull(ret);
        assertEquals(2, ret.getLineItemList().size());
    }

    @Test
    public void testGetLineItemsBadRequest() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

    @Test
    public void testGetLineItemsException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class))).thenThrow(new RuntimeException("connection failure"));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.getLineItems(ltiToken, ltiContextEntity));
    }

    @Test
    public void testDeleteLineItemUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testPutLineItemUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechAdvantageAgsService.putLineItem(ltiToken, ltiContextEntity, lineItem));
    }

    @Test
    public void testGetLineItemUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechAdvantageAgsService.getLineItem(ltiToken, ltiContextEntity, "lineItemId"));
    }

    @Test
    public void testPostLineItemsUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechAdvantageAgsService.postLineItems(ltiToken, ltiContextEntity, lineItems));
    }

    @Test
    public void testPostScoreSuccess() {
        // A real Score instance is used here (rather than the inherited `score` mock) because
        // postScore() serializes it with a real (non-mocked) JsonMapper call. Serializing a raw
        // Mockito mock can throw (byte-buddy mocks expose extra non-bean properties Jackson chokes
        // on), which would make this "success" test spuriously fall into the catch block instead.
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        assertDoesNotThrow(() -> oneEdTechAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, LTI_URL, realScore));
    }

    @Test
    public void testPostScoreBadRequest() {
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(400)));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, LTI_URL, realScore));
    }

    @Test
    public void testPostScoreException() {
        Score realScore = Score.builder().userId("1").scoreGiven(1F).scoreMaximum(1F).activityProgress("Completed").gradingProgress("FullyGraded").build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class))).thenThrow(new RuntimeException("connection failure"));

        assertThrows(ConnectionException.class, () -> oneEdTechAdvantageAgsService.postScore(ltiToken, ltiToken, ltiContextEntity, LTI_URL, realScore));
    }

}
