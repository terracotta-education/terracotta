package edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.utils.LtiStrings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("unchecked")
public class OneEdTechAdvantageDeepLinkServiceImplTest extends BaseTest {

    private static final String ISSUER = "https://lms.example.com";
    private static final String CLIENT_ID = "client-id-123";
    private static final String LOCAL_URL = "https://terracotta.example.com";
    private static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDovlSkSB1FnbHR4ROh2/kVxQtpRQQKK7iYIEpRh3veKF2j2eiSvAc0GbOx596L62KEXGNUrBeW1UR1d9UfP40qsm6yP8MM1knv8eGDyCpGfcUSEysWXBBs9yUbILdBBO3XBNP3zd21mQUGTDjOXQppjDm9JnKNGxVjmFQiMdsBx8nHDeLt+QygGRgHcQmpmiY7kXZl3r5CIoLY+hvtC67byhES/2fQ1XWoHvikRRBt2GHT93jZXJtgjA89ZOsSPqb6LTh1OZIqS12pYciS7TQ9zbeN2SwYfW+G9o8f8bAZxZz6iZVCUmwC6ZOaHZ8KKM7FauddhaXxW/70MdQqgTg/AgMBAAECggEAITyk+7zsqTdm4HEDC7dNL+Wuxn67n/Q0bU0XL+NoNgaPsMl6pBHD+ZW+CqbxKgwYSoyjBsF4sOqN1zSgs9CwiStoEX53jUrAzko9iUM5fk2Rqg4gthW5psX4f5JBeUCJ8o3W82lrwvYyOH8EEbxJs176E9/8tdfrSwjC4ws5mlw24LqXZ5GEJl1nwlAjGv1h3JPf2A457WXkpq0jZ4YejKbdJIzFLgF6B6HpDLHWjcJXeNAhjuiDxWqmsqqO41qOR+sUvTDPEVpdEB5sBWZxr0a3lJ1ZyXbYaZb6LSdtMJghk0CKNhVEpm2z4TZElQfaii3oG1oaQo4YlQ0zL4vmcQKBgQD6EvRcy+yMTBJi9G34CWbVFoAMrw7RsYkRmqPvdCY7eidn43fu0n/z+mw7bBtnmMZNaZs+HvaX3JqTTEXaj19W0lJg92OWK1sWur2T/GPw1GVVSm9niOP1RR4cxXYQqCQF3y9V89wRZ4X+K+F5i6rar3rgAu+U03adKJnPTHzF2QKBgQDuQj4Cehn3gpEAocWfLQeOQNbBfjBYWOmyHC2UQpXnuLCW3/zvcIssQEJTWGpX5556aXDs/SN/yJax+8E6MzwNC4HC4xU+3tBfJ3DqABR9U5Gb+EZ/lLKCXodLsxddB4TPVbivjC+LHN6/Vpj9Rsnl6m7+wgDkbkccnVhjfUsn1wKBgHZFDqLwowA7XhrExVmggKzYxli5VkXgNBZKT6wI/6fzfr2IfAlMLs2hqxxzZYaaX3bvMkev9yodYFG3qfXTBuEV+XX4qnW0LZFTYiOiI1Yb7Yzn9kY+HKm8NaCf1tXL37WTN1zsRzFIB7wM3sdQQc7JXVCistJtLFTphczfvMcJAoGBAMalgi/sf5PmT2E4f50sHP2Uv7kJreMrFoVCixnuvi85xDm2vJshuVeGqAX3VIq/+VjUaqucjqluo333ie4tY2b47hJ/5GnLue1r4++la2/maiOhR539ayvZBnKt+c+9ghSfwuDSP517z5e16s5Y4+KGqE5NkBLkgvOvmE8y2qN3AoGAcsKq+UWOYsi9LtmG6oM0w6S749z180j6zT/axzVowxfULNRXEzhqOSZTfe7zIAbhc09S68oliJdWkzlVRfPYyiL9oq5+gl/writb8if+VyeTdlJJsThfkGGrD8PiJoqMGIby8uzs7/EIPNWRiP/32DS58+6a5xAojVV0PHaM/mI=-----END PRIVATE KEY-----";

    @InjectMocks private OneEdTechAdvantageDeepLinkServiceImpl oneEdTechAdvantageDeepLinkService;

    private final Jws<Claims> idToken = mock(Jws.class);
    private final Claims claims = mock(Claims.class);

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        when(idToken.getPayload()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn(ISSUER);
        when(claims.getAudience()).thenReturn(Set.of(CLIENT_ID));
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("nonce-value");
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID), eq(String.class))).thenReturn("deployment-id");
        when(claims.get(eq(LtiStrings.DEEP_LINKING_SETTINGS), eq(Map.class))).thenReturn(Map.of(LtiStrings.DEEP_LINK_DATA, "opaque-data"));

        when(platformDeployment.getLocalUrl()).thenReturn(LOCAL_URL);
        List<PlatformDeployment> deployments = List.of(platformDeployment);
        when(platformDeploymentRepository.findByIssAndClientId(ISSUER, CLIENT_ID)).thenReturn(deployments);

        when(ltiDataService.getOwnPrivateKey()).thenReturn(PRIVATE_KEY);
    }

    private Map<String, Object> decodeClaims(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        return new ObjectMapper().readValue(payloadJson, Map.class);
    }

    @Test
    public void testGenerateDeepLinkJwt() throws Exception {
        DeepLinkJwtDto dto = oneEdTechAdvantageDeepLinkService.generateDeepLinkJwt(List.of("request-1"), idToken, "https://lms.example.com/return");

        assertNotNull(dto);
        assertNotNull(dto.getJwt());
        assertEquals("https://lms.example.com/return", dto.getReturnUrl());

        Map<String, Object> jwtClaims = decodeClaims(dto.getJwt());
        assertEquals(CLIENT_ID, jwtClaims.get("iss"));
        assertEquals(ISSUER, jwtClaims.get(LtiStrings.AUD));
        assertEquals("nonce-value", jwtClaims.get(LtiStrings.LTI_NONCE));
        assertEquals(ISSUER, jwtClaims.get(LtiStrings.LTI_AZP));
        assertEquals("deployment-id", jwtClaims.get(LtiStrings.LTI_DEPLOYMENT_ID));
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE, jwtClaims.get(LtiStrings.LTI_MESSAGE_TYPE));
        assertEquals(LtiStrings.LTI_VERSION_3, jwtClaims.get(LtiStrings.LTI_VERSION));
        assertEquals("opaque-data", jwtClaims.get(LtiStrings.LTI_DATA));

        List<Map<String, Object>> contentItems = (List<Map<String, Object>>) jwtClaims.get(LtiStrings.LTI_CONTENT_ITEMS);
        assertEquals(1, contentItems.size());
        Map<String, Object> contentItem = contentItems.get(0);
        assertEquals(LtiStrings.DEEP_LINK_LTIRESOURCELINK, contentItem.get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("Terracotta", contentItem.get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(String.format("%s/lti3", LOCAL_URL), contentItem.get(LtiStrings.DEEP_LINK_URL));
    }

    @Test
    public void testGenerateDeepLinkJwtUsesFirstMatchingPlatformDeployment() throws Exception {
        PlatformDeployment secondDeployment = mock(PlatformDeployment.class);
        when(platformDeploymentRepository.findByIssAndClientId(ISSUER, CLIENT_ID)).thenReturn(List.of(platformDeployment, secondDeployment));

        DeepLinkJwtDto dto = oneEdTechAdvantageDeepLinkService.generateDeepLinkJwt(List.of("request-1"), idToken, "https://lms.example.com/return");

        Map<String, Object> jwtClaims = decodeClaims(dto.getJwt());
        List<Map<String, Object>> contentItems = (List<Map<String, Object>>) jwtClaims.get(LtiStrings.LTI_CONTENT_ITEMS);
        assertEquals(String.format("%s/lti3", LOCAL_URL), contentItems.get(0).get(LtiStrings.DEEP_LINK_URL));
        // second (unused) deployment's URL should never be consulted
        verify(secondDeployment, never()).getLocalUrl();
    }

    @Test
    public void testGenerateLtiDeepLinkThrowsUnsupportedOperationException() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThrows(
            UnsupportedOperationException.class,
            () -> oneEdTechAdvantageDeepLinkService.generateLtiDeepLink(null, request, "state")
        );
    }

    @Test
    public void testFindByUuidThrowsUnsupportedOperationException() {
        UUID uuid = UUID.randomUUID();

        assertThrows(
            UnsupportedOperationException.class,
            () -> oneEdTechAdvantageDeepLinkService.findByUuid(uuid)
        );
    }

    @Test
    public void testDeleteThrowsUnsupportedOperationException() {
        LtiDeepLink ltiDeepLink = new LtiDeepLink();

        assertThrows(
            UnsupportedOperationException.class,
            () -> oneEdTechAdvantageDeepLinkService.delete(ltiDeepLink)
        );
    }

}
