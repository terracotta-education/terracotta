package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AdvantageConnectorHelperImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private AdvantageConnectorHelperImpl advantageConnectorHelper;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    public void testCreateRequestEntity() {
        HttpEntity ret = advantageConnectorHelper.createRequestEntity("apiKey");
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + "apiKey", headers.get(HttpHeaders.AUTHORIZATION).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityLtiToken() {
        HttpEntity ret = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityLtiTokenLineItem() {
        HttpEntity<LineItem> ret = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, lineItem);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityLtiTokenLineItems() {
        HttpEntity<LineItems> ret = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, lineItems);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityLtiTokenString() {
        HttpEntity<String> ret = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, "score");
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals("application/vnd.ims.lis.v1.score+json", headers.get(HttpHeaders.CONTENT_TYPE).get(0));
    }

    @Test
    public void testGetTokenSuccess() throws ConnectionException, GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(ltiToken));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        LtiToken ret = advantageConnectorHelper.getToken(platformDeployment, "scope");

        assertNotNull(ret);
    }

    @Test
    public void testGetTokenFailure() throws GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenThrow(new RuntimeException("Error"));

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));
    }

    @Test
    public void testNextPage() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("link", Collections.singletonList("<https://example.com/page2>; rel=\"next\""));
        String ret = advantageConnectorHelper.nextPage(headers);

        assertEquals("https://example.com/page2", ret);
    }

    @Test
    public void testNextPageNoNextPage() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("link", Collections.singletonList("<https://example.com/page2>; rel=\"prev\""));
        String ret = advantageConnectorHelper.nextPage(headers);

        assertNull(ret);
    }

    @Test
    public void testCreateRestTemplate() {
        RestTemplate ret = advantageConnectorHelper.createRestTemplate();

        assertEquals(BufferingClientHttpRequestFactory.class, ret.getRequestFactory().getClass());
    }
}