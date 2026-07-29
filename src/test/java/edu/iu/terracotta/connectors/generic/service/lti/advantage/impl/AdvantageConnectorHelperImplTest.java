package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public void testCreateTokenizedRequestEntityWithAccept() {
        HttpEntity ret = advantageConnectorHelper.createTokenizedRequestEntityWithAccept(ltiToken, MediaType.APPLICATION_JSON_VALUE);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.get(HttpHeaders.ACCEPT).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityWithAcceptAndContentType() {
        HttpEntity ret = advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(ltiToken, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.get(HttpHeaders.ACCEPT).get(0));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.get(HttpHeaders.CONTENT_TYPE).get(0));
    }

    @Test
    public void testCreateTokenizedRequestEntityWithAcceptAndContentTypeLineItem() {
        HttpEntity<LineItem> ret = advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(ltiToken, lineItem, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE);
        HttpHeaders headers = ret.getHeaders();

        assertEquals(TextConstants.BEARER + ltiToken.getAccess_token(), headers.get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.get(HttpHeaders.ACCEPT).get(0));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(lineItem, ret.getBody());
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
    public void testGetTokenNullResponseThrowsConnectionException() throws GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(null);
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));
    }

    @Test
    public void testGetTokenNonSuccessStatusThrowsConnectionException() throws GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));
    }

    @Test
    public void testGetTokenFailure() throws GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenThrow(new RuntimeException("Error"));

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));
    }

    @Test
    public void testGetTokenCachesResultForSameScope() throws ConnectionException, GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(ltiToken.getExpires_in()).thenReturn(3600);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(ltiToken));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        LtiToken first = advantageConnectorHelper.getToken(platformDeployment, "scope");
        LtiToken second = advantageConnectorHelper.getToken(platformDeployment, "scope");

        assertEquals(first, second);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    public void testGetTokenDoesNotCacheAcrossDifferentScopes() throws ConnectionException, GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(ltiToken.getExpires_in()).thenReturn(3600);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(ltiToken));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        advantageConnectorHelper.getToken(platformDeployment, "scope1");
        advantageConnectorHelper.getToken(platformDeployment, "scope2");

        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    public void testGetTokenRefetchesAfterExpiry() throws ConnectionException, GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        // expires_in of 1 second, minus the 60 second safety buffer, is already in the past
        when(ltiToken.getExpires_in()).thenReturn(1);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(ltiToken));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        advantageConnectorHelper.getToken(platformDeployment, "scope");
        advantageConnectorHelper.getToken(platformDeployment, "scope");

        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    public void testGetTokenFailureDoesNotCacheAnything() throws GeneralSecurityException, IOException {
        when(ltiJwtService.generateTokenRequestJWT(any(PlatformDeployment.class))).thenReturn("jwt");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenThrow(new RuntimeException("Error"));
        doReturn(restTemplate).when(advantageConnectorHelper).createRestTemplate();

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));
        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "scope"));

        // each failed getToken() call retries once internally (form-encoded, then JSON), so two
        // calls to getToken() mean four postForEntity() invocations - the point being neither
        // failure gets cached, so both outer calls actually attempt the request from scratch
        verify(restTemplate, times(4)).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
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
    public void testNextPageNoLinkHeader() {
        HttpHeaders headers = new HttpHeaders();
        String ret = advantageConnectorHelper.nextPage(headers);

        assertNull(ret);
    }

    @Test
    public void testCreateRestTemplate() {
        RestTemplate ret = advantageConnectorHelper.createRestTemplate();

        assertEquals(BufferingClientHttpRequestFactory.class, ret.getRequestFactory().getClass());
    }

    // a hung/slow LMS response must not block a thread indefinitely - init() (normally run by
    // Spring's @PostConstruct, called manually here since these are plain Mockito tests) must
    // apply the configured connect/read timeouts to the shared RestTemplate's request factory.
    @Test
    public void testInitConfiguresRequestFactoryTimeouts() {
        org.springframework.test.util.ReflectionTestUtils.setField(advantageConnectorHelper, "connectTimeoutMs", 1234);
        org.springframework.test.util.ReflectionTestUtils.setField(advantageConnectorHelper, "readTimeoutMs", 5678);

        advantageConnectorHelper.init();

        RestTemplate ret = advantageConnectorHelper.createRestTemplate();
        Object requestFactory = org.springframework.test.util.ReflectionTestUtils.getField(ret.getRequestFactory(), "requestFactory");

        assertEquals(1234, org.springframework.test.util.ReflectionTestUtils.getField(requestFactory, "connectTimeout"));
        assertEquals(5678, org.springframework.test.util.ReflectionTestUtils.getField(requestFactory, "readTimeout"));
    }
}