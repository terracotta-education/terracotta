package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.utils.LtiStrings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@SuppressWarnings("unchecked")
public class CanvasAdvantageDeepLinkServiceImplTest extends BaseTest {

    private static final String ISSUER = "https://canvas.example.edu";
    private static final String CLIENT_ID = "clientId123";

    // Valid PKCS#8 RSA test-only private key (also used by CanvasApiJwtServiceImplTest).
    private static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDovlSkSB1FnbHR4ROh2/kVxQtpRQQKK7iYIEpRh3veKF2j2eiSvAc0GbOx596L62KEXGNUrBeW1UR1d9UfP40qsm6yP8MM1knv8eGDyCpGfcUSEysWXBBs9yUbILdBBO3XBNP3zd21mQUGTDjOXQppjDm9JnKNGxVjmFQiMdsBx8nHDeLt+QygGRgHcQmpmiY7kXZl3r5CIoLY+hvtC67byhES/2fQ1XWoHvikRRBt2GHT93jZXJtgjA89ZOsSPqb6LTh1OZIqS12pYciS7TQ9zbeN2SwYfW+G9o8f8bAZxZz6iZVCUmwC6ZOaHZ8KKM7FauddhaXxW/70MdQqgTg/AgMBAAECggEAITyk+7zsqTdm4HEDC7dNL+Wuxn67n/Q0bU0XL+NoNgaPsMl6pBHD+ZW+CqbxKgwYSoyjBsF4sOqN1zSgs9CwiStoEX53jUrAzko9iUM5fk2Rqg4gthW5psX4f5JBeUCJ8o3W82lrwvYyOH8EEbxJs176E9/8tdfrSwjC4ws5mlw24LqXZ5GEJl1nwlAjGv1h3JPf2A457WXkpq0jZ4YejKbdJIzFLgF6B6HpDLHWjcJXeNAhjuiDxWqmsqqO41qOR+sUvTDPEVpdEB5sBWZxr0a3lJ1ZyXbYaZb6LSdtMJghk0CKNhVEpm2z4TZElQfaii3oG1oaQo4YlQ0zL4vmcQKBgQD6EvRcy+yMTBJi9G34CWbVFoAMrw7RsYkRmqPvdCY7eidn43fu0n/z+mw7bBtnmMZNaZs+HvaX3JqTTEXaj19W0lJg92OWK1sWur2T/GPw1GVVSm9niOP1RR4cxXYQqCQF3y9V89wRZ4X+K+F5i6rar3rgAu+U03adKJnPTHzF2QKBgQDuQj4Cehn3gpEAocWfLQeOQNbBfjBYWOmyHC2UQpXnuLCW3/zvcIssQEJTWGpX5556aXDs/SN/yJax+8E6MzwNC4HC4xU+3tBfJ3DqABR9U5Gb+EZ/lLKCXodLsxddB4TPVbivjC+LHN6/Vpj9Rsnl6m7+wgDkbkccnVhjfUsn1wKBgHZFDqLwowA7XhrExVmggKzYxli5VkXgNBZKT6wI/6fzfr2IfAlMLs2hqxxzZYaaX3bvMkev9yodYFG3qfXTBuEV+XX4qnW0LZFTYiOiI1Yb7Yzn9kY+HKm8NaCf1tXL37WTN1zsRzFIB7wM3sdQQc7JXVCistJtLFTphczfvMcJAoGBAMalgi/sf5PmT2E4f50sHP2Uv7kJreMrFoVCixnuvi85xDm2vJshuVeGqAX3VIq/+VjUaqucjqluo333ie4tY2b47hJ/5GnLue1r4++la2/maiOhR539ayvZBnKt+c+9ghSfwuDSP517z5e16s5Y4+KGqE5NkBLkgvOvmE8y2qN3AoGAcsKq+UWOYsi9LtmG6oM0w6S749z180j6zT/axzVowxfULNRXEzhqOSZTfe7zIAbhc09S68oliJdWkzlVRfPYyiL9oq5+gl/writb8if+VyeTdlJJsThfkGGrD8PiJoqMGIby8uzs7/EIPNWRiP/32DS58+6a5xAojVV0PHaM/mI=-----END PRIVATE KEY-----";

    @InjectMocks private CanvasAdvantageDeepLinkServiceImpl canvasAdvantageDeepLinkService;

    private Jws<Claims> idToken;
    private Claims claims;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        claims = mock(Claims.class);
        idToken = mock(Jws.class);
        when(idToken.getPayload()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn(ISSUER);
        when(claims.getAudience()).thenReturn(Set.of(CLIENT_ID));
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("nonce123");
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID), eq(String.class))).thenReturn("deployment123");

        when(platformDeploymentRepository.findByIssAndClientId(ISSUER, CLIENT_ID)).thenReturn(List.of(platformDeployment));
        when(ltiDataService.getOwnPrivateKey()).thenReturn(PRIVATE_KEY);
    }

    private Map<String, Object> decodeJwtPayload(String jwt) throws Exception {
        String payloadSegment = jwt.split("\\.")[1];
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadSegment));

        return new ObjectMapper().readValue(payloadJson, new TypeReference<Map<String, Object>>() { });
    }

    @Test
    public void testGenerateDeepLinkJwtWithDeepLinkData() throws Exception {
        Map<String, Object> deepLinkSettings = Map.of(LtiStrings.DEEP_LINK_DATA, "opaque-data-blob");
        when(claims.get(eq(LtiStrings.DEEP_LINKING_SETTINGS), eq(Map.class))).thenReturn(deepLinkSettings);

        DeepLinkJwtDto ret = canvasAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://return.example.edu");

        assertNotNull(ret);
        assertEquals("https://return.example.edu", ret.getReturnUrl());
        assertNotNull(ret.getJwt());

        Map<String, Object> payload = decodeJwtPayload(ret.getJwt());
        assertEquals(CLIENT_ID, payload.get("iss"));
        assertEquals("opaque-data-blob", payload.get(LtiStrings.LTI_DATA));
        assertEquals("deployment123", payload.get(LtiStrings.LTI_DEPLOYMENT_ID));
        assertEquals("nonce123", payload.get(LtiStrings.LTI_NONCE));
        assertEquals(ISSUER, payload.get(LtiStrings.LTI_AZP));
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE, payload.get(LtiStrings.LTI_MESSAGE_TYPE));
        assertEquals(LtiStrings.LTI_VERSION_3, payload.get(LtiStrings.LTI_VERSION));

        List<Map<String, Object>> contentItems = (List<Map<String, Object>>) payload.get(LtiStrings.LTI_CONTENT_ITEMS);
        assertEquals(1, contentItems.size());

        Map<String, Object> deepLink = contentItems.get(0);
        assertEquals(LtiStrings.DEEP_LINK_LTIRESOURCELINK, deepLink.get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("Terracotta", deepLink.get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals("http://lti.url/lti3", deepLink.get(LtiStrings.DEEP_LINK_URL));

        Map<String, Object> custom = (Map<String, Object>) deepLink.get("custom");
        assertEquals("$User.username", custom.get("canvas_login_id"));
        assertEquals("$User.username", custom.get("canvas_user_name"));
    }

    @Test
    public void testGenerateDeepLinkJwtWithoutDeepLinkSettings() throws Exception {
        when(claims.get(eq(LtiStrings.DEEP_LINKING_SETTINGS), eq(Map.class))).thenReturn(null);

        DeepLinkJwtDto ret = canvasAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://return.example.edu");

        Map<String, Object> payload = decodeJwtPayload(ret.getJwt());
        assertNull(payload.get(LtiStrings.LTI_DATA));
    }

    @Test
    public void testGenerateDeepLinkJwtNoPlatformDeploymentFound() {
        when(platformDeploymentRepository.findByIssAndClientId(ISSUER, CLIENT_ID)).thenReturn(List.of());

        TerracottaConnectorException exception = assertThrows(
            TerracottaConnectorException.class,
            () -> canvasAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://return.example.edu")
        );

        assertEquals(
            String.format("No PlatformDeployment found for issuer [%s] and clientId [%s]", ISSUER, CLIENT_ID),
            exception.getMessage()
        );
    }

    @Test
    public void testGenerateDeepLinkJwtInvalidPrivateKeyFormat() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-pem-key");
        when(claims.get(eq(LtiStrings.DEEP_LINKING_SETTINGS), eq(Map.class))).thenReturn(null);

        assertThrows(
            GeneralSecurityException.class,
            () -> canvasAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://return.example.edu")
        );
    }

    @Test
    public void testGenerateLtiDeepLink() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.generateLtiDeepLink(lti3Request, httpServletRequest, "state")
        );
    }

    @Test
    public void testFindByUuid() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.findByUuid(UUID.randomUUID())
        );
    }

    @Test
    public void testDelete() {
        LtiDeepLink ltiDeepLink = mock(LtiDeepLink.class);

        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.delete(ltiDeepLink)
        );
    }

}
